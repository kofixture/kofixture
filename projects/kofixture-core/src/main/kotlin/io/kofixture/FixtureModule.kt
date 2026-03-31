package io.kofixture

import io.kotest.property.Arb
import kotlin.collections.set
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName
import kotlin.reflect.typeOf

class FixtureModule internal constructor(
    private val registrations: List<RegistryBuilder.() -> Unit>,
) {
    fun applyTo(builder: RegistryBuilder) {
        registrations.forEach { it(builder) }
    }
}

class FixtureModuleBuilder {
    @PublishedApi
    internal val registrations = mutableListOf<RegistryBuilder.() -> Unit>()

    inline fun <reified T> register(generator: Generator<T>) {
        registrations.add { register<T>(generator) }
    }

    inline fun <reified T> register(provider: T) {
        registrations.add { register<T>(Generator { provider }) }
    }

    @JvmName("registerGeneratorFactory")
    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    inline fun <reified T> register(noinline factory: RegistrationScope.() -> Generator<T>) {
        registrations.add { register<T>(factory) }
    }

    @JvmName("registerArbFactory")
    @OptIn(ExperimentalTypeInference::class)
    @OverloadResolutionByLambdaReturnType
    inline fun <reified T> register(noinline factory: RegistrationScope.() -> Arb<T>) {
        registrations.add { register<T>(factory) }
    }

    fun build(): FixtureModule = FixtureModule(registrations.toList())
}

fun fixtureModule(block: FixtureModuleBuilder.() -> Unit): FixtureModule = FixtureModuleBuilder().apply(block).build()
