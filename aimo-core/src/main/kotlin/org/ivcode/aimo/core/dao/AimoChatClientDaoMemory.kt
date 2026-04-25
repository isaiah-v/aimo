package org.ivcode.aimo.core.dao

import java.util.UUID

class AimoChatClientDaoMemory: AimoChatClientDao {
    
    private val sessions: MutableMap<UUID, ChatSessionEntity> = mutableMapOf()
    private val requests: MutableMap<UUID, MutableList<ChatRequestEntity>> = mutableMapOf()
    
    override fun createChatSession(): ChatSessionEntity {
        val chatId = UUID.randomUUID()
        val session = ChatSessionEntity(
            chatId = chatId,
            metadata = mapOf()
        )
        
        sessions[chatId] = session
        return session
    }

    override fun createChatSession(metadata: Map<String, String>): ChatSessionEntity {
        val chatId = UUID.randomUUID()
        val session = ChatSessionEntity(
            chatId = chatId,
            metadata = metadata
        )

        sessions[chatId] = session
        return session
    }

    override fun getChatSessions(): List<ChatSessionEntity> {
        return sessions.values.toList()
    }

    override fun getChatSession(chatId: UUID): ChatSessionEntity? {
        return sessions[chatId]
    }

    override fun getChatSession(
        chatId: UUID,
        metadata: Map<String, String>
    ): ChatSessionEntity? {
        val session = sessions[chatId] ?: return null
        
        for(md in metadata) {
            val sessionValue = session.metadata[md.key] ?: return null
            if (sessionValue != md.value) return null
        }
        
        return session
    }

    override fun deleteChatSession(chatId: UUID): Boolean {
        return sessions.remove(chatId) != null
    }

    override fun deleteChatSession(
        chatId: UUID,
        metadata: Map<String, String>
    ): Boolean {
        val session = sessions[chatId] ?: return false
        
        for(md in metadata) {
            val sessionValue = session.metadata[md.key] ?: return false
            if (sessionValue != md.value) return false
        }
        
        return sessions.remove(chatId) != null
    }

    override fun addChatRequest(request: ChatRequestEntity) {
        // Add the request to the list of requests for the chat, creating the list if needed
        val list = requests.getOrPut(request.chatId) { mutableListOf() }
        list.add(request)
    }

    override fun getChatRequests(chatId: UUID): List<ChatRequestEntity> {
        return requests[chatId]?.toList() ?: emptyList()
    }

    override fun getMessages(chatId: UUID): List<ChatMessageEntity> {
        // Flatten messages from all requests for the chat in insertion order
        return requests[chatId]?.flatMap { it.messages } ?: emptyList()
    }

    override fun getMessages(chatId: UUID, maxRequestCharacters: Int): List<ChatMessageEntity> {
        if (maxRequestCharacters <= 0) {
            return emptyList()
        }

        val chatRequests = requests[chatId] ?: return emptyList()
        if (chatRequests.isEmpty()) {
            return emptyList()
        }

        var totalCharacters = 0
        val selected = mutableListOf<ChatRequestEntity>()

        // Pick newest requests first until the cumulative character budget would be exceeded.
        for (request in chatRequests.asReversed()) {
            if (totalCharacters + request.requestCharacters > maxRequestCharacters) {
                break
            }

            selected.add(request)
            totalCharacters += request.requestCharacters
        }

        return selected.asReversed().flatMap { it.messages }
    }

    override fun upsertSessionMetadata(
        chatId: UUID,
        metadata: Map<String, Any>
    ): Boolean {
        // If the session does not exist we do not create one; return false to indicate
        // the operation was not performed per the user's requirement.
        val existing = sessions[chatId] ?: return false

        // If metadata is empty, nothing to do; treat as successful no-op
        if (metadata.isEmpty()) return true

        // Merge existing metadata with provided metadata (provided keys override)
        val merged = existing.metadata.toMutableMap()
        for ((k, v) in metadata) {
            merged[k] = v
        }
        sessions[chatId] = existing.copy(metadata = merged.toMap())
        return true
    }

    override fun deleteSessionMetadata(
        chatId: UUID,
        keys: List<String>
    ): Boolean {
        // If the session does not exist, indicate failure
        val existing = sessions[chatId] ?: return false

        // If no keys provided, nothing to do; treat as successful no-op
        if (keys.isEmpty()) return true

        val updated = existing.metadata.toMutableMap()
        for (k in keys) {
            updated.remove(k)
        }

        sessions[chatId] = existing.copy(metadata = updated.toMap())
        return true
    }
}
