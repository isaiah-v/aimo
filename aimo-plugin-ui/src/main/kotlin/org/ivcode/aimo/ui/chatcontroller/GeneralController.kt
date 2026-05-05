package org.ivcode.aimo.ui.chatcontroller

import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.SystemMessage

@ChatController
class GeneralController {

    @SystemMessage
    val formatting = "Formatted content as Markdown"
}