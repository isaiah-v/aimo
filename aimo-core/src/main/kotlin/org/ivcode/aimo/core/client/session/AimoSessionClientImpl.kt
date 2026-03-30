package org.ivcode.aimo.core.client.session

import org.ivcode.aimo.core.AimoChatClient
import org.ivcode.aimo.core.AimoSessionClient
import org.ivcode.aimo.core.PromptFactory
import org.ivcode.aimo.core.client.chat.AimoChatClientImpl
import org.ivcode.aimo.core.controller.SystemMessageCallback
import org.ivcode.aimo.core.dao.AimoChatClientDao
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.tool.ToolCallback
import java.util.UUID

internal class AimoSessionClientImpl (
    override val chatId: UUID,
    private val dao: AimoChatClientDao,
    private val chatModel: ChatModel,
    private val promptFactory: PromptFactory,
    private val tools: List<ToolCallback>,
    private val systemMessages: List<SystemMessageCallback>,
    metadata: Map<String, Any>
) : AimoSessionClient {

    private val metadata: MutableMap<String, Any> = metadata.toMutableMap()

    override fun createChatClient(): AimoChatClient {
        return AimoChatClientImpl (
            chatId = chatId,
            session = this,
            dao = dao,
            chatModel = chatModel,
            promptFactory = promptFactory,
            tools = tools,
            systemMessages = systemMessages,
        )
    }

    override fun getMetadata(): Map<String, Any> {
        return metadata.toMap()
    }

    override fun readMetadata(): Map<String, Any> {
        return dao.getChatSession(chatId)?.let { session ->
            replaceAllMetadata(session.metadata)
            metadata.toMap()
        } ?: throw IllegalStateException("Chat session not found for chatId: $chatId")
    }

    override fun getProperty(property: String): Any? {
        return metadata[property]
    }

    override fun readProperty(property: String): Any? {
        return readMetadata()[property]
    }

    override fun writeProperty(property: String, value: Any) {
        dao.upsertSessionMetadata(chatId, mapOf(property to value))
        putMetadata(property, value)
    }

    override fun deleteProperty(property: String): Boolean {
        val deleted = dao.deleteSessionMetadata(chatId, listOf(property))
        if (deleted) removeMetadata(property)
        return deleted
    }

    @Synchronized
    private fun replaceAllMetadata(metadata: Map<String, Any>) {
        this.metadata.clear()
        this.metadata.putAll(metadata)
    }

    @Synchronized
    private fun putMetadata(name: String, value: Any) {
        this.metadata[name] = value
    }

    @Synchronized
    private fun removeMetadata(name: String) {
        this.metadata.remove(name)
    }
}