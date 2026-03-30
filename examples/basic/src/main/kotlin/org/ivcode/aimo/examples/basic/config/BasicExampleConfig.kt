package org.ivcode.aimo.examples.basic.config

import org.ivcode.aimo.core.dao.AimoChatClientDao
import org.ivcode.aimo.core.dao.AimoChatClientDaoMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BasicExampleConfig {
    @Bean
    fun createAimoDao(): AimoChatClientDao {
        return AimoChatClientDaoMemory()
    }
}
