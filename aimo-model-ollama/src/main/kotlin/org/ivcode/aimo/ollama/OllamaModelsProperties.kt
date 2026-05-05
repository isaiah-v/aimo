package org.ivcode.aimo.ollama

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aimo.model")
data class OllamaModelsProperties(
    val ollama: Map<String, OllamaModelProperties> = emptyMap(),
)

data class OllamaModelProperties(
    val baseUrl: String = "http://localhost:11434",
    val primary: Boolean = false,
    val contextSize: Int = 8192,
    val options: Map<String, Any> = emptyMap(),
)

