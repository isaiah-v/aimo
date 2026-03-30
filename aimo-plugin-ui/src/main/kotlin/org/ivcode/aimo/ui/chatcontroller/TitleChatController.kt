package org.ivcode.aimo.ui.chatcontroller

import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.SystemMessage
import org.ivcode.aimo.core.controller.Tool
import org.ivcode.aimo.core.util.getSessionClient
import org.ivcode.aimo.server.extentions.setTitle
import org.springframework.ai.chat.model.ToolContext

@ChatController
class TitleChatController {

    @SystemMessage
    fun titleSystemMessage(currentTitle: String?): String  = """
        The current title is ${currentTitle ?: "not set"}. If the title is not appropriate, you can change it using
        the setTitle tool."
    """.trimIndent()

    @Tool(name = "setTitle", description = "Set the title of the chat session.")
    fun setTitle(title: String, context: ToolContext): String {
        val sessionClient = context.getSessionClient() ?: throw IllegalStateException("Title cannot be set. No session client found in context")
        sessionClient.setTitle(title)

        return "The title has been set to $title"
    }
}