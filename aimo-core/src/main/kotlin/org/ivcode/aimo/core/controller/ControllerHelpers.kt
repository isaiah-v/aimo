package org.ivcode.aimo.core.controller

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

internal fun toToolCallbacks(controller: Any): List<ToolCallback> {
    if (!hasToolAnnotations(controller)) {
        return emptyList()
    }

    val provider = MethodToolCallbackProvider.builder()
        .toolObjects(controller)
        .build()

    return provider.toolCallbacks.toList()
}

private fun hasToolAnnotations(controller: Any): Boolean {
    var type: Class<*>? = controller::class.java

    while (type != null && type != Any::class.java) {
        if (type.declaredMethods.any {
                it.isAnnotationPresent(org.springframework.ai.tool.annotation.Tool::class.java)
            }) {
            return true
        }

        type = type.superclass
    }

    return false
}

/**
 * Scan the given controller instance and return callbacks for fields and methods
 * annotated with @SystemMessage.
 *
 * Rules:
 * - Fields annotated with @SystemMessage will be used as-is (their toString() is returned).
 * - Kotlin properties annotated with @SystemMessage are supported.
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

    // Kotlin properties
    for (property in controller::class.memberProperties) {
        if (property.findAnnotation<SystemMessage>() == null) continue
        callbacks += PropertySystemMessageCallback(controller, property)
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
            1 -> {
                if (params[0] != SystemMessageContext::class.java) {
                    throw IllegalStateException("Method ${method.name} in ${clazz.name} annotated with @SystemMessage has invalid parameters. Must have either no parameters or a single parameter of type SystemMessageContext.")
                }
                true
            }
            else -> {
                // invalid signature
                throw IllegalStateException("Method ${method.name} in ${clazz.name} annotated with @SystemMessage has invalid parameters. Must have either no parameters or a single parameter of type SystemMessageContext.")
            }
        }

        // attempt to make method accessible; if it fails we will surface a clearer error when invoking
        trySetAccessible(method)

        callbacks += MethodSystemMessageCallback(controller, method, isContextual)
    }

    return callbacks
}
