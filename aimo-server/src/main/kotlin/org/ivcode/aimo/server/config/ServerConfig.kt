package org.ivcode.aimo.server.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.ivcode.aimo.server.controller.ChatController
import org.ivcode.aimo.server.controller.HistoryController
import org.ivcode.aimo.server.controller.SessionController
import org.ivcode.aimo.server.service.ChatService
import org.ivcode.aimo.server.service.HistoryService
import org.ivcode.aimo.server.service.SessionService

@Configuration
@Import(value = [
    ChatController::class,
    ChatService::class,
    HistoryController::class,
    HistoryService::class,
    SessionController::class,
    SessionService::class
])
class ServerConfig {
    init {
        println("init")
    }
}