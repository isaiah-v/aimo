package org.ivcode.aimo.core.dao

import java.time.Instant
import java.util.UUID

data class ChatSessionEntity (
    val chatId: UUID,
    val metadata: Map<String, Any>
)

data class ChatRequestEntity (
    val chatId: UUID,
    val requestId: Int,
    val messages: List<ChatMessageEntity>
)

data class ChatMessageEntity (
    val chatId: UUID,
    val requestId: Int,
    val messageId: Int,
    val type: String,
    val content: String?,
    val thinking: String?,
    val toolName: String?,
    val createdAt: Instant,
)