package org.ivcode.aimo.ui.chatcontroller

import org.ivcode.aimo.core.controller.ChatController
import org.ivcode.aimo.core.controller.Tool
import org.ivcode.aimo.server.util.getRequestMetadata
import org.ivcode.aimo.ui.model.TimeResponse
import org.springframework.ai.chat.model.ToolContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit



private const val HEADER_X_TIMEZONE_OFFSET = "x-timezone-offset"

@ChatController
class TimeChatController {

    @Tool(
        name ="current_time",
        description = "Returns current server time and, when x-timezone-offset is provided, inferred user-local time"
    )
    fun currentTime(context: ToolContext): TimeResponse {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        val userZoneId = context.getTimezoneOffset()?.let {
            toZoneId(it)
        }

        return TimeResponse(
            system = now.atZone(ZoneId.systemDefault()).toString(),
            user = userZoneId?.let { now.atZone(it).toString() },
        )
    }

    fun ToolContext.getTimezoneOffset(): Int? {
        return getRequestMetadata()?.let {
            return it.headers[HEADER_X_TIMEZONE_OFFSET]?.firstOrNull()?.toIntOrNull()
        }
    }

    fun toZoneId(timezoneOffset: Int): ZoneId? {
        val zoneOffset = ZoneOffset.ofTotalSeconds(-1 * timezoneOffset * 60)
        return ZoneId.ofOffset("UTC", zoneOffset)
    }
}