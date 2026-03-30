package org.ivcode.aimo.core.util

import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.controller.SystemMessageContext
import org.springframework.ai.chat.model.ToolContext
import java.util.UUID

internal const val CONTEXT_KEY__CHAT_ID = "chatId"
internal const val CONTEXT_KEY__REQUEST_ID = "requestId"
internal const val CONTEXT_KEY__SESSION = "session-client"

fun SystemMessageContext.getChatId(): UUID? = this.context[CONTEXT_KEY__CHAT_ID] as UUID?
fun ToolContext.getChatId(): UUID? = this.context[CONTEXT_KEY__CHAT_ID] as UUID?

fun SystemMessageContext.getRequestId(): Int? = this.context[CONTEXT_KEY__REQUEST_ID] as Int?
fun ToolContext.getRequestId(): Int? = this.context[CONTEXT_KEY__REQUEST_ID] as Int?

fun SystemMessageContext.getSessionClient(): AimoSessionClient? = this.context[CONTEXT_KEY__SESSION] as? AimoSessionClient
fun ToolContext.getSessionClient(): AimoSessionClient? = this.context[CONTEXT_KEY__SESSION] as? AimoSessionClient