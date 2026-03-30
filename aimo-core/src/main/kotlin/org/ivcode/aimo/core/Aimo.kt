package org.ivcode.aimo.core

import java.util.UUID

interface Aimo {
    fun getSessionClient(chatId: UUID): AimoSessionClient?
    fun createSession(): AimoSession
    fun getSessions(): List<AimoSession>
    fun deleteSession(chatId: UUID): Boolean
    fun getChatHistory(chatId: UUID): List<AimoHistoryRequest>
    
    /**
     * Update the metadata for an existing session identified by [chatId], or insert the metadata
     * if the session already exists.
     *
     * Important: this method does NOT create a new session if one does not already exist. It
     * performs an "upsert" only against an existing session — implementations should return
     * `false` when no session with the given [chatId] exists and therefore no upsert was
     * performed.
     *
     * Implementations may choose whether the update is atomic and whether concurrent updates
     * are merged or overwritten; consult the concrete implementation for exact semantics.
     *
     * @param chatId the identifier of the session to update
     * @param metadata the metadata map to apply to the existing session
     * @return `true` if the metadata was applied to an existing session; `false` if the session
     *         does not exist and no upsert was performed
     */
    fun upsertSession(chatId: UUID, metadata: Map<String, String>): Boolean
}