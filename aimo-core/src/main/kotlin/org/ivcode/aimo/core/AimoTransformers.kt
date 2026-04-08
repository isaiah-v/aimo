package org.ivcode.aimo.core

import org.ivcode.aimo.core.dao.ChatMessageEntity
import org.ivcode.aimo.core.dao.ChatRequestEntity
import org.ivcode.aimo.core.dao.ChatSessionEntity

internal fun ChatSessionEntity.toAimoSession(): AimoSession = AimoSession (
    chatId = chatId,
    metadata = metadata
)

internal fun ChatRequestEntity.toAimoHistoryRequest(): AimoHistoryRequest = AimoHistoryRequest(
    chatId = chatId,
    requestId = requestId,
    messages = messages.map { it.toAimoChatMessage() },
    createdAt = createdAt
)

private fun ChatMessageEntity.toAimoChatMessage() = AimoChatMessage (
    messageId = messageId,
    type = type.toAimoChatMessageType(),
    content = content,
    thinking = thinking,
    toolName = toolName,
)

private fun String.toAimoChatMessageType(): AimoChatMessageType = when(this) {
    "SYSTEM" -> AimoChatMessageType.SYSTEM
    "USER" -> AimoChatMessageType.USER
    "ASSISTANT" -> AimoChatMessageType.ASSISTANT
    "TOOL" -> AimoChatMessageType.TOOL
    else -> throw IllegalArgumentException("Unknown message type: $this")
}