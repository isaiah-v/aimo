package org.ivcode.aimo.ollama

import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OllamaConfig {

    @Bean
    fun createOllamaApi() = OllamaApi.Builder().build()

    @Bean
    fun createOllamaChatModel(
        api: OllamaApi
    ) = OllamaChatModel.builder()
        .ollamaApi(api)
        .build()

    @Bean
    fun createPromptFactory() = OllamaPromptFactory("gpt-oss:20b")
}
