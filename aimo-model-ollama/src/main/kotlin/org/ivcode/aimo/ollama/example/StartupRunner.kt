package org.ivcode.aimo.ollama.example

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.core.AimoChatRequest
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component


@Component
class StartupRunner(
    private val aimo: Aimo? = null,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        println("Application started - running StartupRunner")

        if (aimo == null) {
            println("Aimo bean not available; StartupRunner will exit. Provide an Aimo bean to enable chat mode.")
            return
        }

        val entity = aimo.createSession()
        val chatClient = aimo.getSessionClient(entity.chatId)!!.createChatClient()
        println("Type your message and press Enter to send. Type 'exit' to quit.")

        // Loop asking the user for input before each request
        while (true) {
            print("\nYou: ")
            val userInput = readLine() ?: break
            val message = userInput.trim()
            if (message.equals("exit", ignoreCase = true)) {
                println("Exiting StartupRunner.")
                break
            }
            if (message.isEmpty()) {
                // skip empty input and prompt again
                continue
            }

            var isThinking = false
            var isResponding = false

            chatClient.chatStream(AimoChatRequest(
                message,
                emptyMap()
            )) { response ->
                response.messages.forEach { message ->
                    val thinking = message.thinking
                    if (!isThinking && !thinking.isNullOrBlank()) {
                        println("\nThinking:")
                        isThinking = true
                        isResponding = false
                    }
                    if (!thinking.isNullOrBlank()) {
                        print(thinking)
                    }

                    val content = message.content
                    if (!isResponding && !content.isNullOrBlank()) {
                        println("\n\nResponse:")
                        isResponding = true
                        isThinking = false
                    }
                    if (!content.isNullOrBlank()) {
                        print(content)
                    }
                }
            }
            println()

            // After sending a message, continue the loop and prompt for the next input.
            // The stream callback will print streaming output asynchronously.
        }
    }
}


