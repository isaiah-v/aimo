package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.dao.ChatMessageEntity
import java.util.UUID

internal fun AimoChatMessage.toChatMessageEntity(
    responseId: UUID
) = ChatMessageEntity (
    requestId = responseId,
    messageId = messageId,
    type = type.name,
    content = content,
    thinking = thinking,
    toolName = toolName,
)

internal fun ChatMessageEntity.toAimoChatMessage() = AimoChatMessage(
    messageId = messageId,
    type = type.let { AimoChatMessageType.valueOf(it) },
    content = content,
    thinking = thinking,
    toolName = toolName,
)
