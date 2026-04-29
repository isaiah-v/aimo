package org.ivcode.aimo.ollama

import org.ivcode.aimo.core.model.AimoChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OllamaModelsProperties::class)
class OllamaConfig {

    @Bean
    fun createAimoChatModels(
        properties: OllamaModelsProperties,
    ): Map<String, AimoChatModel> {
        val apiByBaseUrl = mutableMapOf<String, OllamaApi>()

        return properties.ollama.mapValues { (name, modelProperties) ->
            val resolvedOptions = resolveOptions(name, modelProperties.options)
            val api = apiByBaseUrl.getOrPut(modelProperties.baseUrl) {
                OllamaApi.Builder().baseUrl(modelProperties.baseUrl).build()
            }

            AimoChatModel(
                name = name,
                chatModel = OllamaChatModel.builder()
                    .ollamaApi(api)
                    .build(),
                options = createChatOptions(resolvedOptions),
                promptFactory = OllamaPromptFactory(resolvedOptions),
                isPrimary = modelProperties.primary,
                contextSize = modelProperties.contextSize,
            )
        }
    }

    @Bean
    fun createPrimaryAimoChatModel(chatModels: Map<String, AimoChatModel>): AimoChatModel {
        val primary = chatModels.values.filter { it.isPrimary }
        require(primary.size <= 1) { "Only one ollama model can be marked primary=true" }

        return primary.firstOrNull()
            ?: chatModels.values.firstOrNull()
            ?: error("No ollama models configured. Define aimo.model.ollama.{name} properties")
    }

    private fun resolveOptions(name: String, rawOptions: Map<String, Any>): Map<String, Any> {
        if (rawOptions.containsKey("model")) return rawOptions
        return LinkedHashMap(rawOptions).also { it["model"] = name }
    }

    private fun createChatOptions(options: Map<String, Any>): ChatOptions {
        val builder = OllamaChatOptions.builder()
        applyRawOptions(builder, options)
        return builder.build()
    }
}
