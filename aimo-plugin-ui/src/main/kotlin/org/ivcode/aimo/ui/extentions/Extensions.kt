package org.ivcode.aimo.ui.extentions

import org.ivcode.aimo.core.AimoSession
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.ui.model.SessionTitle
import java.util.UUID

internal const val PROPERTY_NAME__TITLE: String = "title"

private const val DEFAULT_TITLE_SOURCE = "USER"

private fun toSessionTitle(raw: Any?, chatId: UUID): SessionTitle? {
    return when (raw) {
        null -> null
        is SessionTitle -> raw
        is String -> SessionTitle(chatId = chatId, source = DEFAULT_TITLE_SOURCE, title = raw)
        is Map<*, *> -> {
            val source = (raw["source"] as? String) ?: DEFAULT_TITLE_SOURCE
            val title = raw["title"] as? String
            val parsedChatId = when (val value = raw["chatId"]) {
                is UUID -> value
                is String -> runCatching { UUID.fromString(value) }.getOrNull()
                else -> null
            } ?: chatId

            SessionTitle(chatId = parsedChatId, source = source, title = title)
        }
        else -> null
    }
}

fun AimoSession.getTitle(): SessionTitle? {
    return toSessionTitle(this.metadata[PROPERTY_NAME__TITLE], this.chatId)
}

fun AimoSessionClient.getTitle(): SessionTitle? {
    return toSessionTitle(this.getProperty(PROPERTY_NAME__TITLE), this.chatId)
}

fun AimoSessionClient.setTitle(title: String, source: String = DEFAULT_TITLE_SOURCE): SessionTitle {
    val sessionTitle = SessionTitle(
        chatId = this.chatId,
        source = source,
        title = title
    )

    this.writeProperty(PROPERTY_NAME__TITLE, sessionTitle)
    return sessionTitle
}
