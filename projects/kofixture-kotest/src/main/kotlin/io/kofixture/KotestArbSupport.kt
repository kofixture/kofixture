package io.kofixture

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

fun <T> Generator<T>.toArb(): Arb<T> = arbitrary { next() }

/**
 * Wraps this [Arb] as a [Generator]. A fresh [RandomSource] is created on every
 * [Generator.next] call, making each invocation independent and thread-safe.
 */
fun <T> Arb<T>.toGenerator(): Generator<T> = Generator { sample(RandomSource.default()).value }

/** Returns an [Arb] that produces values of type [T] using this registry. */
inline fun <reified T> Registry.arb(noinline block: OverrideScope.() -> Unit = {}): Arb<T> = generator<T>(block).toArb()

@JvmName("registerArbFactory")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> RegistryBuilder.register(noinline factory: RegistrationScope.() -> Arb<T>) {
    registerFactory(kotlin.reflect.typeOf<T>()) { factory().toGenerator() }
}

@JvmName("fixtureModuleRegisterArbFactory")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> FixtureModuleBuilder.register(noinline factory: RegistrationScope.() -> Arb<T>) {
    addRegistration { register<T>(factory) }
}
