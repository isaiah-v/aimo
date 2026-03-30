package org.ivcode.aimo.server.model

import java.util.UUID

data class NewChatResponse (
    val chatId: String
)

data class ChatSession (
    val chatId: UUID,
    val title: String?
)

data class ChatSessionUpdateRequest (
    val title: String
)