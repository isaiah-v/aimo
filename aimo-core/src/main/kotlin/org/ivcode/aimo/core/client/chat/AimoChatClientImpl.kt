package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatClient
import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoChatRequest
import org.ivcode.aimo.core.AimoChatResponse
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.PromptFactory
import org.ivcode.aimo.core.client.chat.utils.toChatResponse
import org.ivcode.aimo.core.controller.SystemMessageCallback
import org.ivcode.aimo.core.controller.SystemMessageContext
import org.ivcode.aimo.core.dao.AimoChatClientDao
import org.ivcode.aimo.core.dao.ChatRequestEntity
import org.ivcode.aimo.core.toAimoChatMessage
import org.ivcode.aimo.core.toChatMessageEntity
import org.ivcode.aimo.core.util.CONTEXT_KEY__CHAT_ID
import org.ivcode.aimo.core.util.CONTEXT_KEY__REQUEST_ID
import org.ivcode.aimo.core.util.CONTEXT_KEY__SESSION
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.ToolCallback
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.UUID

internal class AimoChatClientImpl (
    override val chatId: UUID,
    private val session: AimoSessionClient,
    private val dao: AimoChatClientDao,
    private val chatModel: ChatModel,
    private val promptFactory: PromptFactory,
    tools: List<ToolCallback>,
    private val systemMessages: List<SystemMessageCallback>,
    maxInputTokens: Int = 4000,
) : AimoChatClient {

    private val initialObservedPromptCharacters: Long = session.getProperty(METADATA_KEY__OBSERVED_PROMPT_CHARACTERS).toNonNegativeLong()
    private val initialObservedPromptTokens: Long = session.getProperty(METADATA_KEY__OBSERVED_PROMPT_TOKENS).toNonNegativeLong()
    private val toolCallbacks: Map<String, ToolCallback> = tools.associateBy { it.toolDefinition.name() }
    private val inputTokenBudgeter = ChatInputTokenBudgeter(
        maxInputTokens = maxInputTokens,
        initialObservedPromptCharacters = initialObservedPromptCharacters,
        initialObservedPromptTokens = initialObservedPromptTokens,
    )

    override fun chat(request: AimoChatRequest): AimoChatResponse {
        return doChat(request, null, this::call)
    }

    override fun chatStream (
        request: AimoChatRequest,
        callback: (AimoChatResponse) -> Unit
    ): AimoChatResponse {
        return doChat(request, callback) { r, m, p -> stream(r, m, p, callback) }
    }

    private fun doChat (
        request: AimoChatRequest,
        callback: ((AimoChatResponse) -> Unit)? = null,
        call: (responseId: UUID, messageId: Int, prompt: Prompt) -> ChatResponse,
    ): AimoChatResponse {
        val historyEntities = dao.getMessages(
            chatId = chatId,
            maxRequestCharacters = inputTokenBudgeter.maxRequestCharactersForLookup(),
        )

        val responseId = UUID.randomUUID()
        val messageStartId = historyEntities
            .lastOrNull()
            ?.messageId
            ?.plus(1)
            ?: 1

        val history = historyEntities.map { it.toAimoChatMessage() }

        val messages = mutableListOf<AimoChatMessage>()

        // add user prompt message
        val promptMessage = createUserMessage(
            messageId = messageId(messageStartId, messages),
            content = request.prompt,
        )

        val systemPromptMessages = getSystemMessages(messageStartId, createSystemMessageContext(responseId, request))

        var response: ChatResponse? = null

        // start the request loop
        while(response == null || response.hasToolCalls()) {
            response = messageId(messageStartId, messages).let { messageId ->

                inputTokenBudgeter.prompt(
                    systemMessages = systemPromptMessages,
                    history = history,
                    prompt = promptMessage,
                    taskMessages = messages,
                    tools = toolCallbacks.values.toList(),
                ) { msgs, tools ->
                    val requestMessages = msgs.withoutThinking()
                    val prompt = promptFactory.create(
                        messages = requestMessages,
                        tools = tools
                    )

                    val response = call(responseId, messageId, prompt)
                    callback?.invoke(createDoneMessage(responseId, messageId))

                    messages.add(response.toAimoChatMessage(messageId))

                    response
                }
            }

            if (response.hasToolCalls()) {
                val toolContext = createToolContext (
                    requestId = responseId,
                    request = request,
                )

                response.result?.output?.toolCalls?.forEach { toolCall ->
                    // TODO: run in parallel

                    val toolCallback = toolCallbacks[toolCall.name]
                    if (toolCallback != null) {
                        val message = try {
                            val toolResponse = toolCallback.call(toolCall.arguments, toolContext)

                            createToolMessage(
                                messageId = messageId(messageStartId, messages),
                                content = toolResponse,
                                toolName = toolCall.name,
                            )
                        } catch (e: Exception) {
                            createToolMessage(
                                messageId = messageId(messageStartId, messages),
                                content = "Error: ${e.message}",
                                toolName = toolCall.name,
                            )
                        }

                        messages.add(message)
                        callback?.onMessage(responseId, message)
                    }
                }
            }
        }

        persistTokenBudgeterCalibration()

        // Compute requestCharacters from persisted messages only, counting content only (excluding thinking).
        // This excludes system messages, history, tools, and thinking metadata which are not persisted per request.
        val requestCharacters = (listOf(promptMessage) + messages)
            .sumOf { msg ->
                // Only count message content, exclude thinking
                msg.content?.length ?: 0
            }

        // write all messages, including the prompt, to the database at the end of the request loop
        dao.addChatRequest(
            ChatRequestEntity(
                chatId = chatId,
                requestId = responseId,
                messages = (listOf(promptMessage) + messages).map { it.toChatMessageEntity(responseId) },
                requestCharacters = requestCharacters,
                createdAt = Instant.now(),
            ))

        // return all messages acquired after the prompt, which includes the assistant response and any tool calls
        return AimoChatResponse(
            chatId = chatId,
            responseId = responseId,
            messages = messages,
            createdAt = Instant.now(),
        )
    }

    private fun getSystemMessages(messageId: Int, context: SystemMessageContext) : List<AimoChatMessage> {
        return systemMessages.mapNotNull { callback ->
            callback.call(context)
        }.map {
            createSystemMessage (
                messageId = messageId,
                content = it
            )
        }
    }

    private fun call(responseId: UUID, messageId: Int, prompt: Prompt): ChatResponse {
        return chatModel.call(prompt)
    }

    private fun stream(responseId: UUID, messageId: Int, prompt: Prompt, callback: (AimoChatResponse) -> Unit): ChatResponse {
        return chatModel.stream(prompt)
            .doCallback(responseId, messageId, callback)
            .toChatResponse()
            .subscribeOn(Schedulers.immediate())
            .block()!!
    }

    private fun messageId(messageStartId: Int, messages: List<AimoChatMessage>): Int = messageStartId + messages.size


    private fun ChatResponse.toAimoChatMessage(messageId: Int, done: Boolean = true): AimoChatMessage {
        val thinking = result?.metadata?.get<String>("thinking")

        return createAssistantMessage(
            messageId = messageId,
            content = result?.output?.text,
            thinking = thinking,
            done = done,
        )
    }

    private fun createSystemMessageContext(requestId: UUID, request: AimoChatRequest) = SystemMessageContext(
        createContextMap (
            requestId = requestId,
            requestContext = request.context,
        )
    )

    private fun createToolContext(requestId: UUID, request: AimoChatRequest): ToolContext = ToolContext(
        createContextMap(
            requestId = requestId,
            requestContext = request.context,
        )
    )

    private fun createContextMap(requestId: UUID, requestContext: Map<String, Any>?): Map<String, Any> {
        val context = mutableMapOf (
            CONTEXT_KEY__CHAT_ID to chatId.toString(),
            CONTEXT_KEY__REQUEST_ID to requestId,
            CONTEXT_KEY__SESSION to session,
        )

        requestContext?.let { context.putAll(it) }
        return context
    }

    private fun MessageType.toAimoChatMessageType(): AimoChatMessageType {
        return when (this) {
            MessageType.USER -> AimoChatMessageType.USER
            MessageType.ASSISTANT -> AimoChatMessageType.ASSISTANT
            MessageType.SYSTEM -> AimoChatMessageType.SYSTEM
            MessageType.TOOL -> AimoChatMessageType.TOOL
        }
    }



    private fun ChatResponse.toAimoStreamResponse(responseId: UUID, messageId: Int): AimoChatResponse {
        return AimoChatResponse(
            chatId = chatId,
            responseId = responseId,
            messages = listOf(toAimoChatMessage(messageId, done = false)),
            createdAt = Instant.now(),
        )
    }

    private fun Flux<ChatResponse>.doCallback(responseId: UUID, messageId: Int, callback: (AimoChatResponse)->Unit): Flux<ChatResponse> {
        return this.doOnNext { r ->
            callback.invoke(r.toAimoStreamResponse(responseId, messageId))
        }
    }

    private fun createDoneMessage(responseId: UUID, messageId: Int) = AimoChatResponse(
        chatId = chatId,
        responseId = responseId,
        messages = listOf(createAssistantMessage(
            messageId = messageId,
            content = null,
            thinking = null,
            done = true,
        )),
        createdAt = Instant.now(),
    )

    fun ((AimoChatResponse)->Unit).onMessage(responseId: UUID, message: AimoChatMessage) {
        invoke(AimoChatResponse(
            chatId = chatId,
            responseId = responseId,
            messages = listOf(message),
            createdAt = Instant.now(),
        ))
    }

    private fun List<AimoChatMessage>.withoutThinking(): List<AimoChatMessage> {
        return map { message ->
            if (message.thinking == null) {
                message
            } else {
                message.copy(thinking = null)
            }
        }
    }


    private fun Any?.toNonNegativeLong(): Long {
        return when (this) {
            is Number -> toLong().coerceAtLeast(0)
            is String -> toLongOrNull()?.coerceAtLeast(0) ?: 0
            else -> 0
        }
    }

    private fun persistTokenBudgeterCalibration() {
        val calibration = inputTokenBudgeter.calibration()
        if (calibration.observedPromptTokens <= 0L) {
            return
        }

        session.writeProperty(METADATA_KEY__OBSERVED_PROMPT_CHARACTERS, calibration.observedPromptCharacters)
        session.writeProperty(METADATA_KEY__OBSERVED_PROMPT_TOKENS, calibration.observedPromptTokens)
    }

    private companion object {
        const val METADATA_KEY__OBSERVED_PROMPT_CHARACTERS = "chat.inputTokenBudgeter.observedPromptCharacters"
        const val METADATA_KEY__OBSERVED_PROMPT_TOKENS = "chat.inputTokenBudgeter.observedPromptTokens"
    }

}