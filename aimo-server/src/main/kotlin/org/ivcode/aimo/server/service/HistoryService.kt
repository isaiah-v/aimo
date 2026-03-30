package org.ivcode.aimo.server.service

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.server.model.ChatHistoryRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HistoryService (
    private val aimo: Aimo
) {
    fun getHistory(chatId: UUID): List<ChatHistoryRequest> {
        return aimo.getChatHistory(chatId).map { it.toChatHistoryRequest() }
    }
}