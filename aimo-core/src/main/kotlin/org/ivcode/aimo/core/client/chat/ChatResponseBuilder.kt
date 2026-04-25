package org.ivcode.aimo.core.client.chat

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import java.util.concurrent.atomic.AtomicReference

/**
 * Collects streaming [ChatResponse] data and aggregates it into a single final response.
 *
 * Properly handles:
 * - Text content streaming (concatenated)
 * - Finish reasons (keeps only the final/last one, not concatenated)
 * - Metadata values (intelligently merged, preserving types)
 * - Tool calls (aggregated as they arrive)
 * - Content filters (deduplicated)
 * - Usage metrics (tokens summed across all chunks)
 */
internal class ChatResponseBuilder {

    private val chatMetadata: MutableMap<String, Any> = mutableMapOf()
    private val chatFinishReason: AtomicReference<String?> = AtomicReference(null)
    private val chatContentFilters: MutableSet<String> = mutableSetOf()
    private val text: StringBuilder = StringBuilder()
    private val toolCalls: MutableList<AssistantMessage.ToolCall> = mutableListOf()
    
    // Usage metrics
    private var promptTokens: Int = 0
    private var completionTokens: Int = 0
    private var totalTokens: Int = 0
    private var nativeUsage: Any? = null

    fun with(response: ChatResponse) = apply {

        response.result?.metadata?.let {
            // Add content filters (Set automatically deduplicates)
            chatContentFilters.addAll(it.contentFilters)

            // Keep ONLY the last non-null finish reason (not concatenated)
            it.finishReason?.let { fr ->
                chatFinishReason.set(fr)
            }

            // Intelligently merge metadata values
            it.entrySet().forEach { entry ->
                val newValue = entry.value
                val existingValue = chatMetadata[entry.key]

                chatMetadata[entry.key] = when {
                    // If both are lists, merge and deduplicate
                    existingValue is List<*> && newValue is List<*> ->
                        (existingValue + (newValue as List<*>)).distinct()
                    // If both are maps, merge them
                    existingValue is Map<*, *> && newValue is Map<*, *> ->
                        existingValue + newValue
                    // If both are strings, concatenate (e.g. thinking blocks streamed in chunks)
                    existingValue is String && newValue is String ->
                        existingValue + newValue
                    // If new value is non-null, use it; otherwise keep existing
                    newValue != null -> newValue
                    else -> existingValue as Any
                }
            }
        }

        // Aggregate usage metrics from response metadata
        response.metadata.let { metadata ->
            metadata.usage.let {
                promptTokens += it.promptTokens
                completionTokens += it.completionTokens
                totalTokens = it.totalTokens
                nativeUsage?.let { negativeUsage -> this.nativeUsage = negativeUsage }
            }
        }

        response.result?.output?.let {
            text.append(it.text)
            toolCalls.addAll(it.toolCalls)
        }
    }

    fun build(): ChatResponse {
        // Build response with aggregated usage metrics
        val responseBuilder = ChatResponse.builder()
            .generations(listOf(
                Generation(
                    AssistantMessage.builder()
                        .content(text.toString())
                        .toolCalls(toolCalls)
                        .build(),
                    ChatGenerationMetadata.builder()
                        .finishReason(chatFinishReason.get())
                        .contentFilters(chatContentFilters)
                        .metadata(chatMetadata)
                        .build()
                )
            ))
        
        // Add aggregated usage metrics to response-level metadata
        if (promptTokens > 0 || completionTokens > 0 || totalTokens > 0) {
            val usageMetadata = ChatResponseMetadata.builder()
                .usage(object : org.springframework.ai.chat.metadata.Usage {
                    override fun getPromptTokens(): Int = this@ChatResponseBuilder.promptTokens
                    override fun getCompletionTokens(): Int = this@ChatResponseBuilder.completionTokens
                    override fun getTotalTokens(): Int = this@ChatResponseBuilder.totalTokens
                    override fun getNativeUsage(): Any? = this@ChatResponseBuilder.nativeUsage
                })
                .build()
            responseBuilder.metadata(usageMetadata)
        } else {
            responseBuilder.metadata(ChatResponseMetadata())
        }
        
        return responseBuilder.build()
    }
}
