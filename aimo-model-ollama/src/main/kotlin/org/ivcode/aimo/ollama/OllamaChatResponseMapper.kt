package org.ivcode.aimo.ollama

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoToolCall
import org.ivcode.aimo.core.model.AimoChatResponseMapper
import org.springframework.ai.chat.model.ChatResponse

class OllamaChatResponseMapper : AimoChatResponseMapper {
    override fun toAimoChatMessage(
        response: ChatResponse,
        messageId: Int,
        done: Boolean,
    ): AimoChatMessage {
        val thinking = response.result?.metadata?.get<String>("thinking")?.takeIf { it.isNotBlank() }
        val toolCalls = response.result?.output?.toolCalls
            ?.map { toolCall ->
                AimoToolCall(
                    id = AimoChatResponseMapper.normalizeToolCallId(toolCall.id, toolCall.name),
                    name = toolCall.name,
                    arguments = toolCall.arguments,
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

