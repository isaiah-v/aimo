package org.ivcode.aimo.core.util

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.AimoChatMessageType
import org.ivcode.aimo.core.AimoSession
import org.ivcode.aimo.core.dao.ChatSessionEntity
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage

fun AimoChatMessage.toSpringAiMessage(): Message {
    return when (this.type) {
        AimoChatMessageType.USER -> toUserMessage()
        AimoChatMessageType.ASSISTANT -> toAssistantMessage()
        AimoChatMessageType.SYSTEM -> toSystemMessage()
        AimoChatMessageType.TOOL -> toToolMessage()
    }
}

private fun AimoChatMessage.toUserMessage(): UserMessage {
    val builder = UserMessage.builder()
    if(this.content!=null) {
        builder.text(this.content)
    }

    return builder.build()
}

private fun AimoChatMessage.toAssistantMessage(): AssistantMessage {
    val builder = AssistantMessage.builder()
    if(this.content!=null) {
        builder.content(this.content)
    }
    if(this.thinking!=null) {
        builder.properties(mapOf("thinking" to this.thinking))
    }

    return builder.build()
}

private fun AimoChatMessage.toSystemMessage(): SystemMessage {
    val builder = SystemMessage.builder()
    if(this.content!=null) {
        builder.text(this.content)
    }

    return builder.build()
}

private fun AimoChatMessage.toToolMessage(): ToolResponseMessage {
    val toolResponse = ToolResponseMessage.ToolResponse (
        "",
        toolName ?: "",
        content ?: ""
    )
    return ToolResponseMessage.builder()
        .responses(listOf(toolResponse))
        .build()
}
