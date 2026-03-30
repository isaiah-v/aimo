package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import java.time.Instant

internal fun createUserMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.USER,
    createdAt: Instant = Instant.now(),
    content: String,
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    createdAt = createdAt,
    content = content,
    thinking = null,
    toolName = null,
)

internal fun createToolMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.TOOL,
    createdAt: Instant = Instant.now(),
    content: String,
    toolName: String,
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    createdAt = createdAt,
    content = content,
    thinking = null,
    toolName = toolName,
)

internal fun createAssistantMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.ASSISTANT,
    createdAt: Instant = Instant.now(),
    content: String?,
    thinking: String?
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    createdAt = createdAt,
    content = content,
    thinking = thinking,
    toolName = null,
)

internal fun createSystemMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.SYSTEM,
    createdAt: Instant = Instant.now(),
    content: String?
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    createdAt = createdAt,
    content = content,
    thinking = null,
    toolName = null,
)