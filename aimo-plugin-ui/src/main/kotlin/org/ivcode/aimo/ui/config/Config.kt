package org.ivcode.aimo.ui.config

import org.ivcode.aimo.ui.chatcontroller.TimeChatController
import org.ivcode.aimo.ui.chatcontroller.TitleChatController
import org.ivcode.aimo.ui.controller.TitleController
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TitleController::class,
    TitleChatController::class,
    TimeChatController::class
)
class Config