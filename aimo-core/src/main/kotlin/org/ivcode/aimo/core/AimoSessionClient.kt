package org.ivcode.aimo.core

import java.util.UUID

/**
 * Represents a client-scoped session for interacting with the AIMO chat/assistant service.
 *
 * Important semantics:
 * - Methods beginning with `get` (for example, [getMetadata], [getProperty]) read from the
 *   in-memory/session cache (fast, may be stale).
 * - Methods beginning with `read` (for example, [readMetadata], [readProperty]) read from the
 *   authoritative datastore (may perform I/O, authoritative).
 * - [writeProperty] will update both the in-memory/session cache and persist the value to the
 *   datastore so subsequent `get*` calls reflect the change and the value is durable in the
 *   persistent store. Implementations may choose to perform persistence synchronously or
 *   asynchronously; callers should consult the concrete implementation for durability guarantees.
 * - [deleteProperty] should remove the value from both the in-memory cache and the datastore.
 *
 * Example:
 * ```kotlin
 * val session: AimoSessionClient = ...
 * // Fast cached read
 * val cached = session.getProperty("userLocale")
 * // Authoritative read from datastore
 * val authoritative = session.readProperty("userLocale")
 * // Update both cache and datastore
 * session.writeProperty("userLocale", "en-US")
 * ```
 */
interface AimoSessionClient {
    /**
     * Unique identifier for the chat associated with this session. Stable for the lifetime
     * of the session and useful for correlating messages, logs, or persisted conversation state.
     */
    val chatId: UUID

    /**
     * Create a chat client bound to this session.
     * @return a new or pooled [AimoChatClient] for performing chat interactions.
     */
    fun createChatClient(): AimoChatClient

    /**
     * Append chat messages to this session's conversation history.
     *
     * Implementations should persist the messages to the session's backing store and update any
     * in-memory/session cache so subsequent reads reflect the appended history.
     *
     * @param messages messages to append, in the order they should appear in the conversation
     */
    fun addMessages(messages: List<AimoChatMessage>)

    /**
     * Return metadata from the in-memory/session cache (fast, may be stale).
     */
    fun getMetadata(): Map<String, Any>

    /**
     * Read metadata from the authoritative datastore (may be slower, authoritative).
     */
    fun readMetadata(): Map<String, Any>

    /**
     * Get a session-level property from the in-memory/session cache (fast, may be stale).
     * @param property the property name to retrieve
     * @return the property value from the cache, or `null` if not present
     */
    fun getProperty(property: String): Any?

    /**
     * Read a session-level property from the authoritative datastore.
     * @param property the property name to read
     * @return the property value from the datastore, or `null` if it does not exist
     */
    fun readProperty(property: String): Any?

    /**
     * Write or update a session-level property.
     *
     * Implementations MUST update both the in-memory/session cache and persist the value to the
     * datastore (synchronously or asynchronously according to the implementation). This ensures
     * that subsequent `get*` reads reflect recent writes and that the value is durable in the
     * persistent store.
     *
     * @param property the property name to set
     * @param value the value to associate with the property
     */
    fun writeProperty(property: String, value: Any)

    /**
     * Delete a session-level property from both the in-memory cache and the datastore.
     * @param property the property name to delete
     * @return `true` if the property was present and removed, `false` if it was not present
     */
    fun deleteProperty(property: String): Boolean
}