package io.kofixture.core

import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

/**
 * Scope for per-sample customisation without mutating the registry.
 *
 * The type parameter [T] is a phantom type used by KSP to generate typed extension
 * properties and functions on specific [OverrideScope] instances, enabling the API:
 *
 * ```kotlin
 * val user by generator<User> { name = "Alice" }
 * ```
 *
 * KSP generates extension properties such as `var OverrideScope<User>.name: String?`
 * that call [addOverride] internally.
 */
class OverrideScope<T>(val registry: FixtureRegistry) {
    @PublishedApi
    internal val fixtureOverrides = mutableListOf<FixtureOverride>()

    private var overriddenCollectionConfig: CollectionConfig? = null

    fun collections(block: CollectionConfigBuilder.() -> Unit) {
        overriddenCollectionConfig = CollectionConfigBuilder().apply(block).build()
    }

    fun addOverride(override: FixtureOverride) {
        fixtureOverrides += override
    }

    fun getOverrides(): List<FixtureOverride> = fixtureOverrides.toList()

    fun getOverriddenCollectionConfig(): CollectionConfig? = overriddenCollectionConfig
}

/** Helper scope available inside type-based override blocks. */
class TypeOverrideScope<T>(val registry: FixtureRegistry) {
    fun gen(block: () -> T): Generator<T> = Generator { _ -> block() }
}

/** Helper scope available inside property-based override blocks. */
class PropOverrideScope<Prop>(val registry: FixtureRegistry) {
    fun gen(block: () -> Prop): Generator<Prop> = Generator { _ -> block() }
}

// ---------------------------------------------------------------------------
// Override DSL extensions — available on any OverrideScope<*>
// ---------------------------------------------------------------------------

@JvmName("override_type")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> OverrideScope<*>.override(crossinline block: TypeOverrideScope<T>.() -> T) {
    addOverride(
        FixtureOverride.TypeBased(
            typeOf<T>(),
            Generator { _ -> TypeOverrideScope<T>(registry).block() },
        ),
    )
}

@JvmName("override_type_gen")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> OverrideScope<*>.override(block: TypeOverrideScope<T>.() -> Generator<T>) {
    addOverride(
        FixtureOverride.TypeBased(
            typeOf<T>(),
            TypeOverrideScope<T>(registry).block(),
        ),
    )
}

@JvmName("override_property")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified Owner : Any, reified Prop> OverrideScope<*>.override(
    property: KProperty1<Owner, Prop>,
    crossinline block: PropOverrideScope<Prop>.() -> Prop,
) {
    addOverride(
        FixtureOverride.Named(
            key = NamedOverrideKey(typeOf<Owner>(), property.name),
            gen = Generator { _ -> PropOverrideScope<Prop>(registry).block() },
        ),
    )
}

@JvmName("override_property_gen")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified Owner : Any, reified Prop> OverrideScope<*>.override(
    property: KProperty1<Owner, Prop>,
    block: PropOverrideScope<Prop>.() -> Generator<Prop>,
) {
    addOverride(
        FixtureOverride.Named(
            key = NamedOverrideKey(typeOf<Owner>(), property.name),
            gen = PropOverrideScope<Prop>(registry).block(),
        ),
    )
}
