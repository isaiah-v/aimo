package org.ivcode.aimo.core.client.chat

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

internal class ChatResponseBuilder {

    private val chatMetadata: MutableMap<String, StringBuilder> = mutableMapOf()
    private val chatFinishReason: StringBuilder = StringBuilder()
    private val chatContentFilters: MutableSet<String> = mutableSetOf()
    private val text: StringBuilder = StringBuilder()
    private val toolCalls: MutableList<AssistantMessage.ToolCall> = mutableListOf()

    fun with(response: ChatResponse) = apply {

        response.result?.metadata?.let {
            chatContentFilters.addAll(it.contentFilters)
            it.finishReason?.let { fr -> chatFinishReason.append(fr) }

            it.entrySet().forEach { entry ->
                val value = chatMetadata.getOrPut(entry.key) { StringBuilder() }
                value.append(entry.value)
            }
        }

        response.result?.output?.let {
            text.append(it.text)
            toolCalls.addAll(it.toolCalls)
        }
    }

    fun build(): ChatResponse {
        return ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder()
                        .content(text.toString())
                        .toolCalls(toolCalls)
                        .build(),
                    ChatGenerationMetadata.builder()
                        .finishReason(chatFinishReason.toString())
                        .contentFilters(chatContentFilters)
                        .metadata(chatMetadata.mapValues { it.value.toString() })
                        .build()
                )
            ))
            .metadata(ChatResponseMetadata())
            .build()
    }
}
