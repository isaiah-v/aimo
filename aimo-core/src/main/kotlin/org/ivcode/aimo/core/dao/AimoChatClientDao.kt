package org.ivcode.aimo.core.dao

import java.util.UUID

interface AimoChatClientDao {

    fun createChatSession(): ChatSessionEntity

    /**
     * Create a new chat session and persist the provided [metadata] with it.
     *
     * The [metadata] map contains initial or additional session information that
     * implementations should store with the newly created session (for example,
     * "user" -> "isaiah" or "tenant" -> "acme"). These metadata entries can
     * later be used to scope or filter lookups (see [getChatSession]) — callers
     * should include any attributes they want persisted or used for future scoping.
     *
     * Implementations must store the metadata as-is (string key/value pairs) and
     * may perform any validation required (e.g., required keys, value formats).
     * This method returns the persisted [ChatSessionEntity] (including any generated
     * identifiers) so the caller can reference the session immediately.
     *
     * Example:
     * val session = createChatSession(mapOf("user" to "isaiah", "locale" to "en-US"))
     * // The returned session will have those metadata entries persisted and future
     * // calls to getChatSession(session.id, mapOf("user" to "isaiah")) should match.
     *
     * @param metadata initial key/value pairs to persist with the new session; must not be null
     * @return the newly created and persisted [ChatSessionEntity]
     */
    fun createChatSession(metadata: Map<String, String>): ChatSessionEntity

    fun getChatSessions(): List<ChatSessionEntity>

    fun getChatSession(chatId: UUID): ChatSessionEntity?

    /**
     * Retrieve a chat session by its [chatId], optionally scoped by the provided [metadata].
     *
     * The implementation should only return a session when all metadata constraints match
     * the stored session's metadata. Metadata acts as a scoping/filtering map: every
     * entry in the provided [metadata] must be present and equal in the stored session's
     * metadata for a match to occur.
     *
     * Metadata is also used to carry initial or additional session information. For example,
     * callers may include attributes that should be stored with the session at creation
     * (e.g. "user", "tenant", or other context). Implementations may persist these
     * values when creating or updating sessions. When provided to this lookup method,
     * however, metadata is treated primarily as a filter: the lookup will only succeed
     * if the stored session's metadata contains matching entries.
     *
     * Example:
     * getChatSession(id, mapOf("user" to "isaiah"))
     * will return a session only if the session's metadata contains the entry
     * "user" -> "isaiah".
     *
     * @param chatId the unique identifier of the chat session to retrieve
     * @param metadata key/value pairs used to scope or filter the lookup. When empty,
     *                 implementations should ignore metadata and match by id only. Implementations
     *                 may also persist metadata as extra session information when creating/updating sessions.
     * @return the matching [ChatSessionEntity] when found and matching the provided metadata,
     *         or null if no matching session exists
     */
    fun getChatSession(chatId: UUID, metadata: Map<String, String>): ChatSessionEntity?

    fun deleteChatSession(chatId: UUID): Boolean
    fun deleteChatSession(chatId: UUID, metadata: Map<String, String>): Boolean


    fun addChatRequest(request: ChatRequestEntity)
    fun getChatRequests(chatId: UUID): List<ChatRequestEntity>
    fun getChatRequests(chatId: UUID, maxRequestCharacters: Int): List<ChatRequestEntity>
    fun getMessages(chatId: UUID): List<ChatMessageEntity>

    
    /**
     * Upsert session metadata for an existing chat session.
     *
     * Performs an "upsert" (insert or update) of the provided key/value metadata for
     * the session identified by [chatId]. Implementations should persist each entry
     * in [metadata] as string key/value pairs so subsequent lookups (for example via
     * [getChatSession]) can observe the stored values.
     *
     * Behavior expectations
     * - Insert or update: if a key from [metadata] does not exist for the session it
     *   should be created; if it already exists it should be updated to the new value.
     * - Atomicity: callers should prefer implementations that provide atomic semantics
     *   (all entries applied or none). If atomic updates are not possible the
     *   implementation should document the guarantees it provides.
     * - Validation: implementations MAY validate keys/values (for example, non-empty
     *   keys) and throw an IllegalArgumentException or a custom validation exception
     *   for invalid input.
     * - Concurrency: concurrent upserts for the same [chatId] are possible;
     *   implementations should document merge/locking behavior and ensure data
     *   integrity according to their storage model.
     *
     * Error handling
     * - Implementations may throw runtime exceptions (e.g., storage-related
     *   exceptions) to indicate failures. Callers should handle such exceptions as
     *   appropriate.
     *
     * Portability notes
     * - The API accepts a Map<String, String> only (no null values). If a caller
     *   needs to remove metadata entries, that should be implemented via a separate
     *   API or convention (not via a null value in this map).
     *
     * Example
     * ```kotlin
     * dao.upsertSessionMetadata(chatId, mapOf("user" to "isaiah", "locale" to "en-US"))
     * ```
     *
     * @param chatId the unique identifier of the chat session whose metadata will be modified
     * @param metadata a non-null map of keys and values to insert or update for the session
     * @return true when the metadata was applied to an existing session, false when the
     *         session identified by [chatId] does not exist and no update was performed
     */
    fun upsertSessionMetadata(chatId: UUID, metadata: Map<String, Any>): Boolean

    fun deleteSessionMetadata(chatId: UUID, keys: List<String>): Boolean
}
