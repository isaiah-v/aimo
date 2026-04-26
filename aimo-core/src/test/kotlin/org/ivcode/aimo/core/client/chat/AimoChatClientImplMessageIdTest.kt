package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatClient
import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatRequest
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.PromptFactory
import org.ivcode.aimo.core.controller.toToolCallbacks
import org.ivcode.aimo.core.dao.AimoChatClientDaoMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.ToolCallback
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            chatModel = FixedResponseChatModel(chatResponse()),
            promptFactory = TestPromptFactory(),
            tools = emptyList(),
            systemMessages = emptyList(),
            maxInputTokens = 1,
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
            chatModel = FixedResponseChatModel(chatResponse()),
            promptFactory = TestPromptFactory(),
            tools = emptyList(),
            systemMessages = emptyList(),
            maxInputTokens = 0,
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
            chatModel = SequencedChatModel(
                responses = listOf(
                    chatResponseWithToolCall(toolName = "echo", arguments = "{\"value\":\"hello\"}"),
                    chatResponse(),
                )
            ),
            promptFactory = TestPromptFactory(),
            tools = toToolCallbacks(TestTools()),
            systemMessages = emptyList(),
            maxInputTokens = 4000,
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
            chatModel = FixedResponseChatModel(chatResponseWithThinking("I thought about it", "the answer")),
            promptFactory = TestPromptFactory(),
            tools = emptyList(),
            systemMessages = emptyList(),
            maxInputTokens = 4000,
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
            chatModel = StreamingChatModel(listOf(
                chatResponseWithThinking("I thought about it", ""),
                chatResponseWithThinking("", " the answer"),
                chatResponse(),
            )),
            promptFactory = TestPromptFactory(),
            tools = emptyList(),
            systemMessages = emptyList(),
            maxInputTokens = 4000,
        )

        client.chatStream(AimoChatRequest(prompt = "think about it", context = emptyMap())) {}

        val assistantMessages = dao.getMessages(chatId).filter { it.type == "ASSISTANT" }
        assertEquals(1, assistantMessages.size)
        assertEquals("I thought about it", assistantMessages.single().thinking)
    }

    @Test
    fun `chatStream done callback includes aggregated content for same message id`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        val client = AimoChatClientImpl(
            chatId = chatId,
            session = TestSessionClient(chatId),
            dao = dao,
            chatModel = StreamingChatModel(listOf(
                chatResponseWithThinking("", "hello"),
                chatResponseWithThinking("", " world"),
            )),
            promptFactory = TestPromptFactory(),
            tools = emptyList(),
            systemMessages = emptyList(),
            maxInputTokens = 4000,
        )

        val callbackResponses = mutableListOf<org.ivcode.aimo.core.AimoChatResponse>()
        client.chatStream(AimoChatRequest(prompt = "stream", context = emptyMap())) { response ->
            callbackResponses.add(response)
        }

        val assistantEvents = callbackResponses
            .flatMap { it.messages }
            .filter { it.type.name == "ASSISTANT" && it.messageId == 2 }

        val doneEvent = assistantEvents.lastOrNull()
        assertNotNull(doneEvent)
        assertEquals(true, doneEvent.done)
        assertEquals("hello world", doneEvent.content)
    }

    private class TestPromptFactory : PromptFactory {
        override fun create(messages: List<AimoChatMessage>, tools: List<ToolCallback>): Prompt {
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

