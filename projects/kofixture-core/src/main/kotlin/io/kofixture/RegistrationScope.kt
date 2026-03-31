package io.kofixture

import kotlin.reflect.typeOf

class RegistrationScope
    @PublishedApi
    internal constructor(
        @PublishedApi internal val registryRef: Lazy<Registry>,
        @PublishedApi internal val context: GenerationContext? = null,
    ) {
        constructor(registryRef: Lazy<Registry>) : this(registryRef, null)

        inline fun <reified T> get(): Generator<T> {
            val type = typeOf<T>()
            val boundContext = context
            if (boundContext != null) {
                return Generator {
                    @Suppress("UNCHECKED_CAST")
                    val override = boundContext.typeOverrides[type] as? Generator<T>
                    override?.next(boundContext) ?: registryRef.value.resolveAndGenerate(type, boundContext)
                }
            }
            return Generator.contextual { currentContext ->
                @Suppress("UNCHECKED_CAST")
                val override = currentContext.typeOverrides[type] as? Generator<T>
                override?.next(currentContext) ?: registryRef.value.resolveAndGenerate(type, currentContext)
            }
        }

        companion object {
            @PublishedApi
            internal fun bound(
                registryRef: Lazy<Registry>,
                context: GenerationContext,
            ): RegistrationScope = RegistrationScope(registryRef, context)
        }
    }
