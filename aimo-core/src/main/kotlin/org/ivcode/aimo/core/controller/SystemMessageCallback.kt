package org.ivcode.aimo.core.controller

interface SystemMessageCallback {
    fun call(context: SystemMessageContext): String?
}