package org.ivcode.aimo.ui.chatcontroller

import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.SystemMessage
import org.ivcode.aimo.core.controller.SystemMessageContext
import org.ivcode.aimo.core.controller.Tool
import org.ivcode.aimo.core.util.getSessionClient
import org.ivcode.aimo.server.extentions.getTitle
import org.ivcode.aimo.server.extentions.setTitle
import org.springframework.ai.chat.model.ToolContext

@ChatController
class TitleChatController {

    @SystemMessage
    fun titleUpdateInstructions(): String  =
        """         
        The chat title can be updated using the tool "setTitle". Update when the title when it doesn't exist or is
        doesn't describe the chat well. The title should be concise, ideally less than 5 words, and should capture the
        essence of the conversation. It should not be too generic, but it also shouldn't be too specific to a single
        message. The title should be updated as the conversation.
        
        The title can be set by the user external from the chat. Use "getTitle" to get the current title of the chat.
        If the title is not set, it should return null.
        """.trimIndent()



    @Tool(name = "setTitle", description = "Set the title of the chat session.")
    fun setTitle(title: String, context: ToolContext): String {
        val sessionClient = context.getSessionClient() ?: throw IllegalStateException("Title cannot be set. No session client found in context")
        sessionClient.setTitle(title)

        return "The title has been set to $title"
    }

    @Tool(name = "getTitle", description = "Gets the title of the chat session.")
    fun getTitle(context: ToolContext): String? {
        return context.getSessionClient()?.getTitle()
    }
}
