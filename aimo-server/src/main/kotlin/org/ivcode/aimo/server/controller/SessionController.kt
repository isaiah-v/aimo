package org.ivcode.aimo.server.controller

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.server.exceptions.NotFoundException
import org.ivcode.aimo.server.model.ChatSession
import org.ivcode.aimo.server.model.ChatSessionUpdateRequest
import org.ivcode.aimo.server.model.NewChatResponse
import org.ivcode.aimo.server.service.SessionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/aimo-api/session")
class SessionController (
    private val chatSessionService: SessionService,
) {

    @PostMapping("/")
    fun createChatSession (
    ): NewChatResponse {
        return chatSessionService.createSession()
    }

    @GetMapping("/")
    fun getChatSessions (): List<ChatSession> {
        return chatSessionService.getSessions()
    }

    @PostMapping("/{chatId}")
    fun updateChatSession(
        @PathVariable chatId: UUID,
        @RequestBody request: ChatSessionUpdateRequest
    ) {
        return chatSessionService.updateSession(chatId, request)
    }

    @DeleteMapping("/{chatId}")
    fun deleteChatSession (
        @PathVariable chatId: UUID
    ) {
        chatSessionService.deleteSession(chatId)
    }
}