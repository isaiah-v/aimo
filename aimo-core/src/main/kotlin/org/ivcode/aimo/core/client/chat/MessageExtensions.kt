package org.ivcode.aimo.core.client.chat

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import sun.font.GlyphLayout.done
import java.time.Instant

internal fun createUserMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.USER,
    content: String,
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    content = content,
    thinking = null,
    toolName = null,
    done = true,
)

internal fun createToolMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.TOOL,
    content: String,
    toolName: String,
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    content = content,
    thinking = null,
    toolName = toolName,
    done = true,
)

internal fun createAssistantMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.ASSISTANT,
    content: String?,
    thinking: String?,
    done: Boolean?,
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    content = content,
    thinking = thinking,
    toolName = null,
    done = done,
)

internal fun createSystemMessage (
    messageId: Int,
    type: AimoChatMessageType = AimoChatMessageType.SYSTEM,
    content: String?
) = AimoChatMessage(
    messageId = messageId,
    type = type,
    content = content,
    thinking = null,
    toolName = null,
    done = true,
)
