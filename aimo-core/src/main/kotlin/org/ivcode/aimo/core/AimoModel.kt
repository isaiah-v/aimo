package org.ivcode.aimo.core

import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.ToolCallback
import java.time.Instant
import java.util.UUID

interface PromptFactory {
    fun create(
        messages: List<AimoChatMessage>,
        tools: List<ToolCallback>
    ): Prompt
}

enum class AimoChatMessageType {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}

data class AimoChatResponse (
    val chatId: UUID,
    val responseId: UUID,
    val messages: List<AimoChatMessage>,
    val createdAt: Instant,
)

data class AimoChatMessage (
    val messageId: Int,
    val type: AimoChatMessageType,
    val content: String?,
    val thinking: String?,
    val toolName: String?,
    val done: Boolean?,
)

data class AimoChatRequest (
    val prompt: String,
    val context: Map<String, Any>,
)

data class AimoHistoryRequest (
    val chatId: UUID,
    val requestId: UUID,
    val messages: List<AimoChatMessage>,
    val createdAt: Instant,
)

data class AimoSession (
    val chatId: UUID,
    val metadata: Map<String, Any>
)
