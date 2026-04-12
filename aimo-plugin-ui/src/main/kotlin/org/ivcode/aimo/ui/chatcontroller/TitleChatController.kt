package org.ivcode.aimo.ui.chatcontroller

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoSession
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.SystemMessage
import org.ivcode.aimo.core.controller.Tool
import org.ivcode.aimo.core.util.getSessionClient
import org.ivcode.aimo.ui.extentions.getTitle
import org.ivcode.aimo.ui.extentions.setTitle
import org.ivcode.aimo.ui.model.SessionTitle
import org.ivcode.aimo.ui.model.TitleResponse
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.annotation.ToolParam
import tools.jackson.databind.ObjectMapper

private const val TITLE_TOOL_NAME = "setTitle"

/**
 * Handles chat title read/write behavior for both tool-driven (LLM) and user-driven updates.
 *
 * Source semantics:
 * - `USER`: title was set by a user action outside the model tool call.
 * - `ASSISTANT`: title was set by the LLM through the `setTitle` tool.
 */
@ChatController
class TitleChatController(
    private val objectMapper: ObjectMapper,
) {

    /**
     * System instructions that guide when and how the model should update a chat title.
     */
    @SystemMessage
    fun titleUpdateInstructions(): String  =
        """         
        Use the "setTitle" tool to update the chat title when it is missing or no longer descriptive.
        Keep titles concise (ideally under 5 words), representative of the conversation, and neither too generic nor
        overly specific to one message. Update the title as the conversation evolves.
        
        A user can set the title externally. Use source "USER" for user-set titles and "ASSISTANT" for LLM-set titles.
        Do not set or overwrite the title if it was already set by the user.
        """.trimIndent()


    /**
     * Tool entrypoint used by the LLM to set a session title.
     * Returns a [TitleResponse] where `source` is `ASSISTANT`.
     */
    @Tool(name = TITLE_TOOL_NAME, description = "Set the chat title. Returns TitleResponse JSON: { title: string, source: \"USER\" | \"ASSISTANT\" } (USER = user-set, ASSISTANT = LLM-set).")
    fun setTitle(
        @ToolParam(description = "The new title") title: String,
        context: ToolContext
    ): TitleResponse {
        val sessionClient = context.getSessionClient() ?: throw IllegalStateException("Title cannot be set. No session client found in context")
        sessionClient.setTitle(title, AimoChatMessageType.ASSISTANT.name)
        return TitleResponse(
            title = title,
            source = AimoChatMessageType.ASSISTANT.name
        )
    }

    /**
     * Sets title for a specific session client and records a TOOL message for model context.
     * Defaults `source` to `USER` for external user-driven title updates.
     */
    fun setTitle(title: String, sessionClient: AimoSessionClient, source: String = AimoChatMessageType.USER.name): TitleResponse {
        sessionClient.setTitle(title, source)
        val response = TitleResponse(
            title = title,
            source = source
        )

        // tell the LLM that the title was set
        sessionClient.addMessages(listOf(
            AimoChatMessage (
                messageId = 1,
                type = AimoChatMessageType.TOOL,
                content = objectMapper.writeValueAsString(response),
                thinking = null,
                toolName = TITLE_TOOL_NAME,
                done = true
            )
        ))

        return response
    }

    /**
     * Tool helper that reads the title from the current tool execution context.
     */
    @Tool(name = "getTitle", description = "Gets the title of the chat session.")
    fun getTitle(context: ToolContext): SessionTitle? {
        return context.getSessionClient()?.getTitle()
    }

    /** Reads the title from the provided session client. */
    fun getTitle(sessionClient: AimoSessionClient): SessionTitle? {
        return sessionClient.getTitle()
    }

    /** Reads the title from the provided session. */
    fun getTitle(session: AimoSession): SessionTitle? {
        return session.getTitle()
    }
}
