package org.ivcode.aimo.core.conf

import org.ivcode.aimo.core.Aimo
import org.ivcode.aimo.core.AimoImpl
import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.ChatControllerEntity
import org.ivcode.aimo.core.controller.SystemMessageCallback
import org.ivcode.aimo.core.controller.toSystemMessageCallbacks
import org.ivcode.aimo.core.controller.toToolCallbacks
import org.ivcode.aimo.core.dao.AimoChatClientDao
import org.ivcode.aimo.core.model.AimoChatModel
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AimoConfig {

    @Bean
    fun createControllerEntities(ctx: ApplicationContext): List<ChatControllerEntity> {
        val list = mutableListOf<ChatControllerEntity>()

        ctx.getBeansWithAnnotation<ChatController>().forEach {(beanName, chatController) ->
            list.add(ChatControllerEntity (
                name = beanName,
                clazz = chatController.javaClass,
                instance = chatController,
                tools = toToolCallbacks(chatController),
                systemMessages = toSystemMessageCallbacks(chatController),
            ))
        }

        return list
    }

    @Bean
    fun createToolCallbacks(chatControllers: List<ChatControllerEntity>): List<ToolCallback> {
        return chatControllers.flatMap { it.tools }
    }

    @Bean
    fun createSystemMessageCallbacks(chatControllers: List<ChatControllerEntity>): List<SystemMessageCallback> {
        return chatControllers.flatMap { it.systemMessages }
    }


    @Bean
    fun createAimo (
        primaryModel: AimoChatModel,
        chatClientDao: AimoChatClientDao,
        tools: List<ToolCallback>,
        systemMessages: List<SystemMessageCallback>,
    ): Aimo {
        return AimoImpl(primaryModel, chatClientDao, tools, systemMessages)
    }
}