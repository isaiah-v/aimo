package org.ivcode.aimo.core.controller

import org.springframework.ai.tool.annotation.Tool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControllerHelpersTest {

    @Test
    fun `toToolCallbacks returns empty list when controller has no tool annotations`() {
        val callbacks = toToolCallbacks(NoToolController())

        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun `toToolCallbacks returns callbacks when controller has tool annotations`() {
        val callbacks = toToolCallbacks(HasToolController())

        assertEquals(1, callbacks.size)
    }

    @Test
    fun `toSystemMessageCallbacks supports plain kotlin property annotation`() {
        val callbacks = toSystemMessageCallbacks(PlainPropertySystemMessageController())

        assertEquals(1, callbacks.size)
        assertEquals("property-system-message", callbacks.first().call(SystemMessageContext(emptyMap())))
    }

    @Test
    fun `toSystemMessageCallbacks still supports field annotation`() {
        val callbacks = toSystemMessageCallbacks(FieldSystemMessageController())

        assertEquals(1, callbacks.size)
        assertEquals("field-system-message", callbacks.first().call(SystemMessageContext(emptyMap())))
    }

    private class NoToolController {
        fun ping(): String = "pong"
    }

    private class HasToolController {
        @Tool(name = "ping", description = "Returns pong")
        fun ping(): String = "pong"
    }

    private class PlainPropertySystemMessageController {
        @SystemMessage
        val message = "property-system-message"
    }

    private class FieldSystemMessageController {
        @field:SystemMessage
        val message = "field-system-message"
    }
}

