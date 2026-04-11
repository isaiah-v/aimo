package org.ivcode.aimo.server.service

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.server.exceptions.NotFoundException
import org.ivcode.aimo.server.model.ChatSession
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SessionService (
    private val aimo: Aimo
) {
    fun createSession(): ChatSession {
        return ChatSession(
            aimo.createSession().chatId
        )
    }

    fun deleteSession(chatId: UUID) {
        if(!aimo.deleteSession(chatId)) {
            throw NotFoundException("Chat session with id $chatId not found")
        }
    }

    fun getSessions(): List<ChatSession> {
        return aimo.getSessions().map { it.toChatSession() }
    }
}
