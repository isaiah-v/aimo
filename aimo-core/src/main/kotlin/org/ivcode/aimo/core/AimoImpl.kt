package org.ivcode.aimo.core

import org.ivcode.aimo.core.client.session.AimoSessionClientImpl
import org.ivcode.aimo.core.controller.SystemMessageCallback
import org.ivcode.aimo.core.dao.AimoChatClientDao
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.tool.ToolCallback
import java.util.UUID

internal class AimoImpl (
    private val chatModel: ChatModel,
    private val promptFactory: PromptFactory,
    private val chatClientDao: AimoChatClientDao,
    private val tools: List<ToolCallback>,
    private val systemMessage: List<SystemMessageCallback>
): Aimo {
    override fun getSessionClient(chatId: UUID): AimoSessionClient? = chatClientDao.getChatSession(chatId)?.let {
        val session = chatClientDao.getChatSession(chatId) ?: return@let null

        AimoSessionClientImpl (
            chatId = it.chatId,
            chatModel = chatModel,
            promptFactory = promptFactory,
            dao = chatClientDao,
            tools = tools,
            systemMessages = systemMessage,
            metadata = session.metadata
        )
    }

    override fun createSession(): AimoSession  {
        return chatClientDao.createChatSession().toAimoSession()
    }

    override fun getSessions(): List<AimoSession> {
        return chatClientDao.getChatSessions().map { it.toAimoSession() }
    }

    override fun deleteSession(chatId: UUID): Boolean {
        return chatClientDao.deleteChatSession(chatId)
    }

    override fun getChatHistory(chatId: UUID): List<AimoHistoryRequest> {
        return chatClientDao.getChatRequests(chatId).map { it.toAimoHistoryRequest() }
    }

    override fun upsertSession(chatId: UUID, metadata: Map<String, String>): Boolean {
        return chatClientDao.upsertSessionMetadata(chatId, metadata)
    }
}