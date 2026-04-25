package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatInputTokenBudgeterTest {

    @Test
    fun `historyForPrompt keeps newest history within default budget`() {
        val budgeter = ChatInputTokenBudgeter(maxInputTokens = 5)
        val history = listOf(
            message(1, "123456789"),
            message(2, "abcdefghi"),
            message(3, "JKLMNOPQR"),
        )

        val result = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )

        assertEquals(listOf(history.last()), result)
    }

    @Test
    fun `historyForPrompt returns empty when prompt and task messages use full budget`() {
        val budgeter = ChatInputTokenBudgeter(maxInputTokens = 6)
        val history = listOf(message(1, "123456789"))

        val result = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, "123456789"),
            taskMessages = listOf(message(100, "abcdefghi")),
            tools = emptyList(),
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun `recordPromptUsage refines token estimate for future requests`() {
        val budgeter = ChatInputTokenBudgeter(maxInputTokens = 4)
        val history = listOf(
            message(1, "123456789"),
            message(2, "abcdefghi"),
        )

        val beforeCalibration = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )
        assertEquals(listOf(history.last()), beforeCalibration)

        budgeter.recordPromptUsage(
            promptMessages = listOf(message(100, "123456789012345678")),
            tools = emptyList(),
            promptTokens = 2,
        )

        val afterCalibration = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )
        assertEquals(history, afterCalibration)
    }


    @Test
    fun `seeded calibration is applied in new budgeter instance`() {
        val seeded = ChatInputTokenBudgeter(
            maxInputTokens = 4,
            initialObservedPromptCharacters = 18,
            initialObservedPromptTokens = 2,
        )
        val history = listOf(
            message(1, "123456789"),
            message(2, "abcdefghi"),
        )

        val result = seeded.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )

        assertEquals(history, result)
    }

    @Test
    fun `prompt refines token estimate from response usage`() {
        val budgeter = ChatInputTokenBudgeter(maxInputTokens = 4)
        val history = listOf(
            message(1, "123456789"),
            message(2, "abcdefghi"),
        )

        val response = budgeter.prompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        ) { requestMessages, requestTools ->
            assertEquals(emptyList(), requestTools)
            assertEquals(listOf(history.last(), message(99, "")), requestMessages)
            chatResponseWithOnePromptToken()
        }

        assertEquals(1, response.metadata.usage.promptTokens)

        val afterCalibration = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )
        assertEquals(history, afterCalibration)
    }

    @Test
    fun `historyForPrompt trims flat history messages from the oldest side`() {
        val budgeter = ChatInputTokenBudgeter(maxInputTokens = 3)
        val history = listOf(
            message(1, "1234"),
            message(2, "1234"),
            message(3, "abcd"),
            message(4, "abcd"),
        )

        val result = budgeter.historyForPrompt(
            systemMessages = emptyList(),
            history = history,
            prompt = message(99, ""),
            taskMessages = emptyList(),
            tools = emptyList(),
        )

        assertEquals(listOf(history[1], history[2], history[3]), result)
    }

    private fun message(id: Int, content: String) = AimoChatMessage(
        messageId = id,
        type = AimoChatMessageType.USER,
        content = content,
        thinking = null,
        toolName = null,
        done = true,
    )

    private fun chatResponseWithOnePromptToken(): ChatResponse {
        return ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder().content("").build(),
                    ChatGenerationMetadata.builder().build()
                )
            ))
            .metadata(
                ChatResponseMetadata.builder()
                    .usage(object : Usage {
                        override fun getPromptTokens(): Int = 1
                        override fun getCompletionTokens(): Int = 0
                        override fun getTotalTokens(): Int = 1
                        override fun getNativeUsage(): Any? = null
                    })
                    .build()
            )
            .build()
    }
}

