package co.kofixtures.core

import kotlin.reflect.KType

data class NamedOverrideKey(
    val ownerType: KType,
    val paramName: String,
)

sealed interface FixtureOverride {
    class TypeBased(
        val type: KType,
        val gen: Generator<*>,
    ) : FixtureOverride

    class Named(
        val key: NamedOverrideKey,
        val gen: Generator<*>,
    ) : FixtureOverride
}
