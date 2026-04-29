package org.ivcode.aimo.core.model

import org.ivcode.aimo.core.PromptFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions

/**
 * Configuration and runtime wiring for a named chat model used by Aimo.
 *
 * @property name Human-readable model identifier.
 * @property chatModel Backing Spring AI chat model implementation.
 * @property options Default chat options (temperature, max tokens, etc.) for this model.
 * @property promptFactory Factory used to build prompts tailored to this model.
 * @property isPrimary Whether this model is the default selection.
 * @property contextSize Approximate maximum context window size, measured in tokens.
 */
data class AimoChatModel (
    val name: String,
    val chatModel: ChatModel,
    val options: ChatOptions,
    val promptFactory: PromptFactory,
    val isPrimary: Boolean = false,
    val contextSize: Int,
)