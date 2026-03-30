package org.ivcode.aimo.core

import java.util.UUID

interface AimoChatClient {
    val chatId: UUID
    fun chat(request: AimoChatRequest): AimoChatResponse
    fun chatStream(request: AimoChatRequest, callback: (AimoChatResponse) -> Unit): AimoChatResponse
}