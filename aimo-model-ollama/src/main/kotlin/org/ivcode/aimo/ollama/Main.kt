package org.ivcode.aimo.ollama

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["org.ivcode.aimo.core", "org.ivcode.aimo.ollama"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

