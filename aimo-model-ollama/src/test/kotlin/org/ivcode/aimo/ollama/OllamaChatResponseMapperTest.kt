package org.ivcode.aimo.ollama

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import kotlin.test.Test
import kotlin.test.assertEquals

class OllamaChatResponseMapperTest {

    private val mapper = OllamaChatResponseMapper()

    @Test
    fun `maps thinking from response metadata`() {
        val response = ChatResponse.builder()
            .generations(
                listOf(
                    Generation(
                        AssistantMessage.builder()
                            .content("answer")
                            .build(),
                        ChatGenerationMetadata.builder()
                            .metadata(mapOf("thinking" to "internal reasoning"))
                            .build()
                    )
                )
            )
            .metadata(emptyMetadata())
            .build()

        val message = mapper.toAimoChatMessage(response, messageId = 2, done = true)

        assertEquals("answer", message.content)
        assertEquals("internal reasoning", message.thinking)
    }

    @Test
    fun `ignores blank thinking metadata`() {
        val response = ChatResponse.builder()
            .generations(
                listOf(
                    Generation(
                        AssistantMessage.builder()
                            .content("answer")
                            .build(),
                        ChatGenerationMetadata.builder()
                            .metadata(mapOf("thinking" to "   "))
                            .build()
                    )
                )
            )
            .metadata(emptyMetadata())
            .build()

        val message = mapper.toAimoChatMessage(response, messageId = 2, done = true)

        assertEquals(null, message.thinking)
    }

    private fun emptyMetadata(): ChatResponseMetadata = ChatResponseMetadata.builder()
        .usage(object : Usage {
            override fun getPromptTokens(): Int = 0
            override fun getCompletionTokens(): Int = 0
            override fun getTotalTokens(): Int = 0
            override fun getNativeUsage(): Any? = null
        })
        .build()
}

