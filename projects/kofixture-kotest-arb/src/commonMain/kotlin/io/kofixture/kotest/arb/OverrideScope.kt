package io.kofixture.kotest.arb

import io.kofixture.core.FixtureOverride
import io.kofixture.core.Generator
import io.kofixture.core.NamedOverrideKey
import io.kofixture.core.OverrideScope
import io.kofixture.core.PropOverrideScope
import io.kofixture.core.TypeOverrideScope
import io.kotest.property.Arb
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

// ---------------------------------------------------------------------------
// Arb-based override extensions — available on any OverrideScope<*>
// These are additive companions to the value/Generator overloads in io.kofixture.core
// ---------------------------------------------------------------------------

/**
 * Registers a type-based override that supplies values via a Kotest [Arb].
 *
 * The [block] runs once at registration time to produce the [Arb], which is then
 * wrapped in an [ArbGenerator] and used for every subsequent sample in this scope.
 */
@JvmName("override_type_arb")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> OverrideScope<*>.override(block: TypeOverrideScope<T>.() -> Arb<T>) {
    addOverride(
        FixtureOverride.TypeBased(
            typeOf<T>(),
            ArbGenerator(TypeOverrideScope<T>(registry).block()),
        ),
    )
}

/**
 * Registers a property-based override for [property] that supplies values via a Kotest [Arb].
 *
 * Only the named property is affected; other properties of the same type are unaffected.
 */
@JvmName("override_property_arb")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified Owner : Any, reified Prop> OverrideScope<*>.override(
    property: KProperty1<Owner, Prop>,
    block: PropOverrideScope<Prop>.() -> Arb<Prop>,
) {
    addOverride(
        FixtureOverride.Named(
            key = NamedOverrideKey(typeOf<Owner>(), property.name),
            gen = ArbGenerator(PropOverrideScope<Prop>(registry).block()),
        ),
    )
}

// ---------------------------------------------------------------------------
// Re-exported value/Generator overloads from io.kofixture.core so that all
// six overloads share the same package scope in io.kofixture.kotest.arb.
// This ensures @OverloadResolutionByLambdaReturnType disambiguation works
// correctly when the call site is also in io.kofixture.kotest.arb.
// ---------------------------------------------------------------------------

@JvmName("override_type_value")
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

@JvmName("override_type_generator")
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

@JvmName("override_property_value")
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

@JvmName("override_property_generator")
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
