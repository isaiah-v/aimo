package org.ivcode.aimo.ui.controller

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.server.consts.API_CONTROLLER_CONTEXT
import org.ivcode.aimo.server.exceptions.NotFoundException
import org.ivcode.aimo.ui.chatcontroller.TitleChatController
import org.ivcode.aimo.ui.model.SessionTitle
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/$API_CONTROLLER_CONTEXT/title")
class TitleController constructor(
    private val aimo: Aimo,
    private val titleChatController: TitleChatController
) {

    @GetMapping("/{chatId}")
    fun getTitle(
        @PathVariable chatId: UUID
    ): SessionTitle? {
        val sessionClient = aimo.getSessionClient(chatId) ?: throw NotFoundException("Chat session with id $chatId not found")
        return titleChatController.getTitle(sessionClient)
    }

    @GetMapping("/")
    fun getTitles(): List<SessionTitle> {
        return aimo.getSessions().mapNotNull { session ->
            titleChatController.getTitle(session)
        }
    }

    @PutMapping("/{chatId}/{title}")
    fun setTitle(
        @PathVariable chatId: UUID,
        @PathVariable title: String
    ) {
        val sessionClient = aimo.getSessionClient(chatId) ?: throw NotFoundException("Chat session with id $chatId not found")
        titleChatController.setTitle(title, sessionClient)
    }
}