package org.ivcode.aimo.core.dao

import java.time.Instant
import java.util.UUID

data class ChatSessionEntity (
    val chatId: UUID,
    val metadata: Map<String, Any>
)

data class ChatRequestEntity (
    val chatId: UUID,
    val requestId: UUID,
    val messages: List<ChatMessageEntity>,
    val createdAt: Instant,
)

data class ChatMessageEntity (
    val requestId: UUID,
    val messageId: Int,
    val type: String,
    val content: String?,
    val thinking: String?,
    val toolName: String?,
)