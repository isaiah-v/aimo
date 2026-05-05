package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatClient
import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoChatRequest
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.PromptFactory
import org.ivcode.aimo.core.AimoToolCall
import org.ivcode.aimo.core.controller.toToolCallbacks
import org.ivcode.aimo.core.dao.AimoChatClientDaoMemory
import org.ivcode.aimo.core.model.AimoChatModel
import org.ivcode.aimo.core.model.AimoChatResponseMapper
import org.ivcode.aimo.core.model.AimoChatResponseMapperFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.ToolCallback
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AimoChatClientImplMessageIdTest {

    @Test
    fun `chat persists sequential message ids across requests`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = FixedResponseChatModel(chatResponse()),
                contextSize = 1,
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chat(AimoChatRequest(prompt = "first request", context = emptyMap()))
        client.chat(AimoChatRequest(prompt = "second request", context = emptyMap()))

        val requestMessageIds = dao.getChatRequests(chatId).map { request -> request.messages.map { it.messageId } }
        assertEquals(listOf(listOf(1, 2), listOf(1, 2)), requestMessageIds)
    }

    @Test
    fun `chat persists sequential ids even when history lookup returns empty`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = FixedResponseChatModel(chatResponse()),
                contextSize = 0,
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chat(AimoChatRequest(prompt = "first request", context = emptyMap()))
        client.chat(AimoChatRequest(prompt = "second request", context = emptyMap()))

        val requestMessageIds = dao.getChatRequests(chatId).map { request -> request.messages.map { it.messageId } }
        assertEquals(listOf(listOf(1, 2), listOf(1, 2)), requestMessageIds)
    }

    @Test
    fun `chat with tool call persists messages in expected order`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = SequencedChatModel(
                    responses = listOf(
                        chatResponseWithToolCall(toolName = "echo", arguments = "{\"value\":\"hello\"}"),
                        chatResponse(),
                    )
                ),
                contextSize = 4000,
            ),
            tools = toToolCallbacks(TestTools()),
            systemMessages = emptyList(),
        )

        val response = client.chat(AimoChatRequest(prompt = "use the tool", context = emptyMap()))

        assertEquals(listOf(2, 3, 4), response.messages.map { it.messageId })
        assertEquals(listOf("ASSISTANT", "TOOL", "ASSISTANT"), response.messages.map { it.type.name })

        val persisted = dao.getMessages(chatId)
        assertEquals(listOf(1, 2, 3, 4), persisted.map { it.messageId })
        assertEquals(listOf("USER", "ASSISTANT", "TOOL", "ASSISTANT"), persisted.map { it.type })
    }

    @Test
    fun `chat persists thinking from assistant response`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = FixedResponseChatModel(chatResponseWithThinking("I thought about it", "the answer")),
                contextSize = 4000,
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chat(AimoChatRequest(prompt = "think about it", context = emptyMap()))

        val assistantMessages = dao.getMessages(chatId).filter { it.type == "ASSISTANT" }
        assertEquals(1, assistantMessages.size)
        assertEquals("I thought about it", assistantMessages.single().thinking)
        assertEquals("the answer", assistantMessages.single().content)
    }

    @Test
    fun `chatStream persists thinking from streamed assistant response`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        // Simulate streaming: first chunk has thinking, second chunk has content, third is empty
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = StreamingChatModel(listOf(
                    chatResponseWithThinking("I thought about it", ""),
                    chatResponseWithThinking("", " the answer"),
                    chatResponse(),
                )),
                contextSize = 4000,
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chatStream(AimoChatRequest(prompt = "think about it", context = emptyMap())) {}

        val assistantMessages = dao.getMessages(chatId).filter { it.type == "ASSISTANT" }
        assertEquals(1, assistantMessages.size)
        assertEquals("I thought about it", assistantMessages.single().thinking)
    }

    @Test
    fun `thinking-only assistant history is not replayed as empty assistant content`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val capturedPrompts = mutableListOf<List<AimoChatMessage>>()

        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = StreamingChatModel(
                    listOf(
                        chatResponseWithThinking("I thought about it", "")
                    )
                ),
                contextSize = 4000,
                promptFactory = RecordingPromptFactory(capturedPrompts),
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chatStream(AimoChatRequest(prompt = "first", context = emptyMap())) {}
        client.chat(AimoChatRequest(prompt = "second", context = emptyMap()))

        assertTrue(capturedPrompts.size >= 2)
        val secondPromptMessages = capturedPrompts[1]
        assertTrue(
            secondPromptMessages.none {
                it.type == AimoChatMessageType.ASSISTANT && it.content.isNullOrBlank() && it.toolCalls.isNullOrEmpty()
            },
            "Thinking-only assistant messages should not be replayed as empty assistant turns"
        )
    }

    @Test
    fun `chat does not persist duplicate tool message when tool call id repeats`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = SequencedChatModel(
                    responses = listOf(
                        chatResponseWithToolCall(toolName = "echo", arguments = "{\"value\":\"hello\"}", toolCallId = "call-1"),
                        chatResponseWithToolCall(toolName = "echo", arguments = "{\"value\":\"hello\"}", toolCallId = "call-1"),
                        chatResponse(),
                    )
                ),
                contextSize = 4000,
            ),
            tools = toToolCallbacks(TestTools()),
            systemMessages = emptyList(),
        )

        client.chat(AimoChatRequest(prompt = "use the tool", context = emptyMap()))

        val toolMessages = dao.getMessages(chatId).filter { it.type == "TOOL" }
        assertEquals(1, toolMessages.size)
        assertEquals("echo", toolMessages.single().toolName)
        assertTrue((toolMessages.single().content ?: "").contains("echo:hello"))
    }

    @Test
    fun `response mapper factory creates a fresh mapper per chat request`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        var createdCount = 0

        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            model = testModel(
                chatModel = FixedResponseChatModel(chatResponse()),
                contextSize = 4000,
                responseMapperFactory = AimoChatResponseMapperFactory {
                    createdCount += 1
                    TestResponseMapper()
                },
            ),
            tools = emptyList(),
            systemMessages = emptyList(),
        )

        client.chat(AimoChatRequest(prompt = "first", context = emptyMap()))
        client.chat(AimoChatRequest(prompt = "second", context = emptyMap()))

        assertEquals(2, createdCount)
    }

    private fun testModel(
        chatModel: ChatModel,
        contextSize: Int,
        responseMapperFactory: AimoChatResponseMapperFactory = AimoChatResponseMapperFactory { TestResponseMapper() },
        promptFactory: PromptFactory = TestPromptFactory(),
    ): AimoChatModel {
        return AimoChatModel(
            name = "test",
            chatModel = chatModel,
            options = TestChatOptions(),
            promptFactory = promptFactory,
            responseMapperFactory = responseMapperFactory,
            contextSize = contextSize,
        )
    }

    private class TestResponseMapper : AimoChatResponseMapper {
        override fun toAimoChatMessage(response: ChatResponse, messageId: Int, done: Boolean): AimoChatMessage {
            val thinking = response.result?.metadata?.get<String>("thinking")?.takeIf { it.isNotBlank() }
            val toolCalls = response.result?.output?.toolCalls
                ?.map {
                    AimoToolCall(
                        id = AimoChatResponseMapper.normalizeToolCallId(it.id, it.name),
                        name = it.name,
                        arguments = it.arguments,
                    )
                }
                ?.takeIf { it.isNotEmpty() }

            return AimoChatMessage(
                messageId = messageId,
                type = AimoChatMessageType.ASSISTANT,
                content = response.result?.output?.text,
                thinking = thinking,
                toolName = null,
                toolCallId = null,
                toolCalls = toolCalls,
                done = done,
            )
        }
    }

    private class TestChatOptions : ChatOptions {
        override fun getModel(): String? = null
        override fun getFrequencyPenalty(): Double? = null
        override fun getMaxTokens(): Int? = null
        override fun getPresencePenalty(): Double? = null
        override fun getStopSequences(): MutableList<String>? = null
        override fun getTemperature(): Double? = null
        override fun getTopK(): Int? = null
        override fun getTopP(): Double? = null

        @Suppress("UNCHECKED_CAST")
        override fun <T : ChatOptions> copy(): T = this as T
    }

    private class TestPromptFactory : PromptFactory {
        override fun create(messages: List<AimoChatMessage>, tools: List<ToolCallback>): Prompt {
            return Prompt(messages.joinToString(separator = "\n") { it.content ?: "" })
        }
    }

    private class RecordingPromptFactory(
        private val capturedPrompts: MutableList<List<AimoChatMessage>>,
    ) : PromptFactory {
        override fun create(messages: List<AimoChatMessage>, tools: List<ToolCallback>): Prompt {
            capturedPrompts += messages.map { it.copy() }
            return Prompt(messages.joinToString(separator = "\n") { it.content ?: "" })
        }
    }

    private class FixedResponseChatModel(
        private val response: ChatResponse,
    ) : ChatModel {
        override fun call(prompt: Prompt): ChatResponse {
            return response
        }

        override fun stream(prompt: Prompt): Flux<ChatResponse> {
            return Flux.just(response)
        }
    }

    private class SequencedChatModel(
        private val responses: List<ChatResponse>,
    ) : ChatModel {
        private var index = 0

        override fun call(prompt: Prompt): ChatResponse {
            val response = responses.getOrElse(index) { responses.last() }
            index += 1
            return response
        }

        override fun stream(prompt: Prompt): Flux<ChatResponse> {
            return Flux.just(call(prompt))
        }
    }

    private class StreamingChatModel(
        private val chunks: List<ChatResponse>,
    ) : ChatModel {
        override fun call(prompt: Prompt): ChatResponse = chunks.last()
        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.fromIterable(chunks)
    }

    private class TestTools {
        @Tool(description = "Echo value")
        fun echo(value: String): String {
            return "echo:$value"
        }
    }

    private class TestSessionClient(
        override val chatId: UUID,
    ) : AimoSessionClient {
        private val metadata = mutableMapOf<String, Any>()

        override fun createChatClient(): AimoChatClient {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override fun addMessages(messages: List<AimoChatMessage>) {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override fun getMetadata(): Map<String, Any> {
            return metadata.toMap()
        }

        override fun readMetadata(): Map<String, Any> {
            return metadata.toMap()
        }

        override fun getProperty(property: String): Any? {
            return metadata[property]
        }

        override fun readProperty(property: String): Any? {
            return metadata[property]
        }

        override fun writeProperty(property: String, value: Any) {
            metadata[property] = value
        }

        override fun deleteProperty(property: String): Boolean {
            return metadata.remove(property) != null
        }
    }

    private fun chatResponse(): ChatResponse {
        return ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder().content("ok").build(),
                    ChatGenerationMetadata.builder().build()
                )
            ))
            .metadata(
                ChatResponseMetadata.builder()
                    .usage(object : Usage {
                        override fun getPromptTokens(): Int = 0
                        override fun getCompletionTokens(): Int = 0
                        override fun getTotalTokens(): Int = 0
                        override fun getNativeUsage(): Any? = null
                    })
                    .build()
            )
            .build()
    }

    private fun chatResponseWithToolCall(toolName: String, arguments: String, toolCallId: String = "call-1"): ChatResponse {
        return ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder()
                        .content("calling tool")
                        .toolCalls(
                            listOf(
                                ToolCall(toolCallId, "function", toolName, arguments)
                            )
                        )
                        .build(),
                    ChatGenerationMetadata.builder().build()
                )
            ))
            .metadata(
                ChatResponseMetadata.builder()
                    .usage(object : Usage {
                        override fun getPromptTokens(): Int = 0
                        override fun getCompletionTokens(): Int = 0
                        override fun getTotalTokens(): Int = 0
                        override fun getNativeUsage(): Any? = null
                    })
                    .build()
            )
            .build()
    }

    private fun chatResponseWithThinking(thinking: String, content: String): ChatResponse {
        return ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder().content(content).build(),
                    ChatGenerationMetadata.builder()
                        .metadata(mapOf("thinking" to thinking))
                        .build()
                )
            ))
            .metadata(
                ChatResponseMetadata.builder()
                    .usage(object : Usage {
                        override fun getPromptTokens(): Int = 0
                        override fun getCompletionTokens(): Int = 0
                        override fun getTotalTokens(): Int = 0
                        override fun getNativeUsage(): Any? = null
                    })
                    .build()
            )
            .build()
    }

}
