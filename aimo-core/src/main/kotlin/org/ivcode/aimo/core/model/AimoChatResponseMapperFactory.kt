package org.ivcode.aimo.core.model

/**
 * Factory for creating [AimoChatResponseMapper] instances.
 *
 * Lifecycle contract:
 * - A new mapper instance must be created for each chat request.
 * - The same mapper instance may be reused only within that single request
 *   (for example, across streamed chunks belonging to that request).
 * - Mapper instances must not be shared across different requests, because
 *   implementations can be stateful (for example, chunk/tag parser state).
 */
fun interface AimoChatResponseMapperFactory {
    fun create(): AimoChatResponseMapper
}
