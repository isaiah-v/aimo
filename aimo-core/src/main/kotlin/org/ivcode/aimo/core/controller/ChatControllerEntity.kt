package org.ivcode.aimo.core.controller

import org.springframework.ai.tool.ToolCallback

data class ChatControllerEntity (
    val name: String,
    val clazz: Class<out Any>,
    val instance: Any,
    val tools: List<ToolCallback>,
    val systemMessages: List<SystemMessageCallback>
)
