package org.ivcode.aimo.server.service

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoChatRequest
import org.ivcode.aimo.core.AimoChatResponse
import org.ivcode.aimo.core.AimoHistoryRequest
import org.ivcode.aimo.core.AimoSession
import org.ivcode.aimo.server.model.ChatHistoryRequest
import org.ivcode.aimo.server.model.ChatMessage
import org.ivcode.aimo.server.model.ChatRequest
import org.ivcode.aimo.server.model.ChatResponse
import org.ivcode.aimo.server.model.ChatSession

internal fun ChatRequest.toAimoChatRequest(context: Map<String, Any> = emptyMap()) = AimoChatRequest (
    prompt = prompt,
    context = context
)

internal fun AimoChatResponse.toChatResponse(): ChatResponse = ChatResponse (
    chatId = chatId,
    responseId = responseId,
    messages = messages.map { it.toChatMessage() },
    createdAt = createdAt
)

internal fun AimoChatMessage.toChatMessage() = ChatMessage(
    messageId = messageId,
    type = type.toRole(),
    content = content,
    thinking = thinking,
    toolName = toolName
)

internal fun AimoChatMessageType.toRole(): ChatMessage.Role = when (this) {
    AimoChatMessageType.USER -> ChatMessage.Role.USER
    AimoChatMessageType.ASSISTANT -> ChatMessage.Role.ASSISTANT
    AimoChatMessageType.SYSTEM -> ChatMessage.Role.SYSTEM
    AimoChatMessageType.TOOL -> ChatMessage.Role.TOOL
}

internal fun AimoHistoryRequest.toChatHistoryRequest() = ChatHistoryRequest(
    chatId = chatId,
    requestId = requestId,
    messages = messages.map { it.toChatMessage() },
    createdAt = createdAt
)

internal fun AimoSession.toChatSession() = ChatSession(
    chatId = chatId,
    title = metadata["title"] as? String
)
