package io.kofixture.kotest.arb

import io.kofixture.core.FactoryScope
import io.kofixture.core.FixtureRegistryBuilder
import io.kofixture.core.Generator
import io.kotest.property.Arb
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

// ---------------------------------------------------------------------------
// Generator-based registration extension kept internal to avoid external overload ambiguity.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("register_generator")
internal inline fun <reified T> FixtureRegistryBuilder.register(
    tag: String? = null,
    noinline factory: FactoryScope.() -> Generator<T>,
) = register(typeOf<T>(), tag, factory)

/**
 * Registers a factory lambda that returns an [Arb] for type [T].
 *
 * The [block] runs at resolve time to produce the [Arb], which is then
 * wrapped in an [ArbGenerator]. The compiler selects this overload when the
 * lambda returns [Arb]&lt;T&gt;, coexisting with the core [register] overload that
 * returns [Generator]&lt;T&gt;.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("register_arb")
inline fun <reified T> FixtureRegistryBuilder.register(
    tag: String? = null,
    noinline block: FactoryScope.() -> Arb<T>,
) {
    register(typeOf<T>(), tag) { ArbGenerator(block(this)) }
}

/** Registers a bare [Arb] directly as the [Generator] for type [T]. */
inline fun <reified T> FixtureRegistryBuilder.registerArb(arb: Arb<T>) {
    register(typeOf<T>(), null) { ArbGenerator(arb) }
}

// ---------------------------------------------------------------------------
// getArb — FactoryScope extension to retrieve an Arb from the registry
// ---------------------------------------------------------------------------

/**
 * Returns an [Arb] for the [property]'s type, resolved from the current scope
 * (respects active overrides, including named overrides for this property).
 */
inline fun <reified Owner : Any, reified Prop> FactoryScope.getArb(
    property: KProperty1<Owner, Prop>,
    tag: String? = null,
): Arb<Prop> = get(property, tag).asArb()
