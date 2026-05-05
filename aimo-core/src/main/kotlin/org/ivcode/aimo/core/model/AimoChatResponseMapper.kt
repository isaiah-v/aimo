package org.ivcode.aimo.core.model

import org.ivcode.aimo.core.AimoChatMessage
import org.springframework.ai.chat.model.ChatResponse
import java.util.UUID

interface AimoChatResponseMapper {
    fun toAimoChatMessage(
        response: ChatResponse,
        messageId: Int,
        done: Boolean = true,
    ): AimoChatMessage

    companion object {
        private val TOOL_CALL_ID_REGEX = Regex("[a-zA-Z0-9_.:-]+")

        fun normalizeToolCallId(rawId: String?, toolName: String): String {
            val cleaned = rawId?.trim().orEmpty()
            if (cleaned.isNotBlank() && cleaned.matches(TOOL_CALL_ID_REGEX)) {
                return cleaned
            }

            val safeToolName = toolName.replace(Regex("[^a-zA-Z0-9_.:-]"), "_")
            return "toolcall-$safeToolName-${UUID.randomUUID()}"
        }
    }
}

