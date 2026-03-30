package org.ivcode.aimo.core.controller

import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.Target

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class ChatController

@Retention(AnnotationRetention.RUNTIME)
@Target(FUNCTION, FIELD, PROPERTY)
annotation class SystemMessage

typealias Tool = Tool