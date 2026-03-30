package org.ivcode.aimo.server.extentions

import org.ivcode.aimo.core.AimoSessionClient

internal const val PROPERTY_NAME__TITLE: String = "title"

fun AimoSessionClient.getTitle(): String? {
    return this.getProperty(PROPERTY_NAME__TITLE) as? String
}

fun AimoSessionClient.setTitle(title: String) {
    this.writeProperty(PROPERTY_NAME__TITLE, title)
}
