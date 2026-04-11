package org.ivcode.aimo.server.controller

import org.ivcode.aimo.server.consts.API_CONTROLLER_CONTEXT
import org.ivcode.aimo.server.model.ChatSession
import org.ivcode.aimo.server.service.SessionService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/$API_CONTROLLER_CONTEXT/session")
class SessionController (
    private val chatSessionService: SessionService,
) {

    @PostMapping("/")
    fun createChatSession (
    ): ChatSession {
        return chatSessionService.createSession()
    }

    @GetMapping("/")
    fun getChatSessions (): List<ChatSession> {
        return chatSessionService.getSessions()
    }

    @DeleteMapping("/{chatId}")
    fun deleteChatSession (
        @PathVariable chatId: UUID
    ) {
        chatSessionService.deleteSession(chatId)
    }
}