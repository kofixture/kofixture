package io.kofixture.kotest.arb

import io.kofixture.core.FactoryScope
import io.kofixture.core.FixtureRegistryBuilder
import io.kofixture.core.Generator
import io.kofixture.core.register
import io.kotest.property.Arb
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

// ---------------------------------------------------------------------------
// Re-exported Generator overload from io.kofixture.core so that both the
// Generator and Arb register overloads share the same package scope in
// io.kofixture.kotest.arb. This enables @OverloadResolutionByLambdaReturnType
// disambiguation at call sites in this package.
// ---------------------------------------------------------------------------

@JvmName("register_generator")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> FixtureRegistryBuilder.register(
    tag: String? = null,
    noinline factory: FactoryScope.() -> Generator<T>,
) = register(typeOf<T>(), tag, factory)

// ---------------------------------------------------------------------------
// Arb-based registration extensions on FixtureRegistryBuilder
// These are additive companions to the Generator overload in io.kofixture.core
// and must coexist with it via @OverloadResolutionByLambdaReturnType.
// ---------------------------------------------------------------------------

/**
 * Registers a factory lambda that returns an [Arb] for type [T].
 *
 * The [block] runs at resolve time to produce the [Arb], which is then
 * wrapped in an [ArbGenerator]. The compiler selects this overload when the
 * lambda returns [Arb]&lt;T&gt;, coexisting with the core [register] overload that
 * returns [Generator]&lt;T&gt;.
 */
@JvmName("register_arb")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> FixtureRegistryBuilder.register(
    tag: String? = null,
    noinline block: FactoryScope.() -> Arb<T>,
) {
    register<T>(tag) { ArbGenerator(block(this)) }
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
