package org.ivcode.aimo.ollama

import org.ivcode.aimo.core.AimoChatMessage
import org.ivcode.aimo.core.PromptFactory
import org.ivcode.aimo.core.util.toSpringAiMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.ai.tool.ToolCallback

class OllamaPromptFactory(
    private val modelName: String,
): PromptFactory {
    override fun create(
        messages: List<AimoChatMessage>,
        tools: List<ToolCallback>
    ): Prompt {
        return Prompt(
            messages.map { message -> message.toSpringAiMessage() },
            OllamaChatOptions.builder()
                .toolCallbacks(*tools.toTypedArray())
                .internalToolExecutionEnabled(false)
                .model(modelName)
                .build()
        )
    }
}