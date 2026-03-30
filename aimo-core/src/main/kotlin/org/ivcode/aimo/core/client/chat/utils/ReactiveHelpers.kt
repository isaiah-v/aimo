package org.ivcode.aimo.core.client.chat.utils

import org.ivcode.aimo.core.client.chat.ChatResponseBuilder
import org.springframework.ai.chat.model.ChatResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal fun Flux<ChatResponse>.toChatResponse(): Mono<ChatResponse> {
    val builder = ChatResponseBuilder()
    return reduce(builder) { b, r -> b.with(r) }.map { it.build() }
}