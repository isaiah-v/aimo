package org.ivcode.aimo.server.model

import java.time.Instant
import java.util.UUID

data class ChatRequest (
    val prompt: String,
    val stream: Boolean = false
)

data class ChatResponse (
    val chatId: UUID,
    val responseId: Int,
    val messages: List<ChatMessage>
)

data class ChatMessage (
    val messageId: Int,
    val type: Role,
    val content: String?,
    val thinking: String?,
    val toolName: String?,
    val createdAt: Instant,
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM,
        TOOL
    }
}

data class ChatHistoryRequest (
    val chatId: UUID,
    val requestId: Int,
    val messages: List<ChatMessage>
)