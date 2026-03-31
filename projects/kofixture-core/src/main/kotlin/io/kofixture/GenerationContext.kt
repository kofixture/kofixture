package io.kofixture

import kotlin.reflect.KProperty1
import kotlin.reflect.KType

// @PublishedApi required: accessed from public inline fun (Registry.next, RegistrationScope.get)
// Not part of the public API — do not use directly.
@PublishedApi internal data class GenerationContext(
    val typeOverrides: Map<KType, Generator<*>>,
    val propertyOverrides: Map<KProperty1<*, *>, Generator<*>>,
) {
    companion object {
        val EMPTY =
            GenerationContext(
                typeOverrides = emptyMap(),
                propertyOverrides = emptyMap(),
            )
    }
}
