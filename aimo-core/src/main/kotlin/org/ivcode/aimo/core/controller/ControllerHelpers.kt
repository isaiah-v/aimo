package org.ivcode.aimo.core.controller

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import java.lang.reflect.Modifier

internal fun toToolCallbacks(controller: Any): List<ToolCallback> {
    val provider = MethodToolCallbackProvider.builder()
        .toolObjects(controller)
        .build()

    return provider.toolCallbacks.toList()
}

/**
 * Scan the given controller instance and return callbacks for fields and methods
 * annotated with @SystemMessage.
 *
 * Rules:
 * - Fields annotated with @SystemMessage will be used as-is (their toString() is returned).
 * - Methods annotated with @SystemMessage must return String? and either take no parameters
 *   or a single parameter of type SystemMessageContext.
 */
internal fun toSystemMessageCallbacks(controller: Any): List<SystemMessageCallback> {
    val callbacks = mutableListOf<SystemMessageCallback>()
    val clazz = controller::class.java

    // Fields
    for (field in clazz.declaredFields) {
        if (field.isAnnotationPresent(SystemMessage::class.java)) {
            // attempt to make field accessible; ignore failure (may be blocked by module system)
            trySetAccessible(field)
            callbacks += FieldSystemMessageCallback(controller, field)
        }
    }

    // Methods
    for (method in clazz.declaredMethods) {
        if (!method.isAnnotationPresent(SystemMessage::class.java)) continue

        // Validate return type: allow java.lang.String or kotlin.String (both are java.lang.String), and allow nullable
        val returnType = method.returnType
        if (returnType != String::class.java) {
            // not a String return type - skip
            continue
        }

        val params = method.parameterTypes
        val isContextual = when (params.size) {
            0 -> false
            1 -> params[0] == SystemMessageContext::class.java
            else -> {
                // invalid signature
                continue
            }
        }

        // attempt to make method accessible; if it fails we will surface a clearer error when invoking
        trySetAccessible(method)

        callbacks += MethodSystemMessageCallback(controller, method, isContextual)
    }

    return callbacks
}


