package org.ivcode.aimo.server.service

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.server.exceptions.NotFoundException
import org.ivcode.aimo.server.model.ChatSession
import org.ivcode.aimo.server.model.ChatSessionUpdateRequest
import org.ivcode.aimo.server.model.NewChatResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SessionService (
    private val aimo: Aimo
) {
    fun createSession(): NewChatResponse {
        return NewChatResponse(aimo.createSession().chatId.toString())
    }

    fun deleteSession(chatId: UUID) {
        if(!aimo.deleteSession(chatId)) {
            throw NotFoundException("Chat session with id $chatId not found")
        }
    }

    fun getSessions(): List<ChatSession> {
        return aimo.getSessions().map { it.toChatSession() }
    }

    fun updateSession(chatId: UUID, request: ChatSessionUpdateRequest) {
        if(!aimo.upsertSession(chatId, mapOf("title" to request.title))) {
            throw NotFoundException("Chat session with id $chatId not found")
        }
    }
}
