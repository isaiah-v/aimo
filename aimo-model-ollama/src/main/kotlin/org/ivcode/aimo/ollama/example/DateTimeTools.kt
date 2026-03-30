package org.ivcode.aimo.ollama.example

import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.SystemMessage
import org.ivcode.aimo.core.controller.Tool
import org.springframework.ai.chat.model.ToolContext

import org.springframework.context.i18n.LocaleContextHolder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ChatController
private class DateTimeTools {

    @SystemMessage
    fun currentTime(): String {
        return "The current time is " + LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString()
    }

    /*
    @get:Tool(description = "Get the current date and time in the user's timezone")
    val currentDateTime: String
        get() = LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString()

     */


    @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
    fun setAlarm(time: String, context: ToolContext): String {
        val alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
        return "Alarm set for " + alarmTime.apply { println(context) }
    }
}