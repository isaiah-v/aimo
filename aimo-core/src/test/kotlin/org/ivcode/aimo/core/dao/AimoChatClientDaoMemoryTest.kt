package org.ivcode.aimo.core.dao

import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.util.UUID

class AimoChatClientDaoMemoryTest {

    @Test
    fun `getMessages with maxRequestCharacters returns newest requests within budget`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId

        dao.addChatRequest(request(chatId, 1, 10, "r1"))
        dao.addChatRequest(request(chatId, 2, 20, "r2"))
        dao.addChatRequest(request(chatId, 3, 30, "r3"))

        val result = dao.getMessages(chatId, maxRequestCharacters = 50)

        assertEquals(listOf("r2", "r3"), result.map { it.content })
    }

    @Test
    fun `getMessages with maxRequestCharacters returns empty for zero or negative budget`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId
        dao.addChatRequest(request(chatId, 1, 10, "r1"))

        assertEquals(emptyList(), dao.getMessages(chatId, maxRequestCharacters = 0))
        assertEquals(emptyList(), dao.getMessages(chatId, maxRequestCharacters = -1))
    }

    @Test
    fun `getMessages with maxRequestCharacters excludes oversized newest request`() {
        val dao = AimoChatClientDaoMemory()
        val chatId = dao.createChatSession().chatId

        dao.addChatRequest(request(chatId, 1, 10, "r1"))
        dao.addChatRequest(request(chatId, 2, 40, "r2"))

        val result = dao.getMessages(chatId, maxRequestCharacters = 30)

        assertEquals(emptyList(), result)
    }

    private fun request(chatId: UUID, messageId: Int, requestCharacters: Int, content: String): ChatRequestEntity {
        val requestId = UUID.randomUUID()
        return ChatRequestEntity(
            chatId = chatId,
            requestId = requestId,
            messages = listOf(
                ChatMessageEntity(
                    requestId = requestId,
                    messageId = messageId,
                    type = "USER",
                    content = content,
                    thinking = null,
                    toolName = null,
                )
            ),
            requestCharacters = requestCharacters,
            createdAt = Instant.now(),
        )
    }
}

