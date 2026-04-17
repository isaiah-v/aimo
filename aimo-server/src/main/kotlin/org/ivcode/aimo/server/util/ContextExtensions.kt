package org.ivcode.aimo.server.util

import org.ivcode.aimo.server.model.RequestMetadata
import org.springframework.ai.chat.model.ToolContext

internal const val PROPERTY_NAME_REQUEST_METADATA = "requestMetadata"

fun ToolContext.getRequestMetadata(): RequestMetadata? {
    return this.context[PROPERTY_NAME_REQUEST_METADATA] as RequestMetadata?
}