package io.kofixture

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

    fun addRegistration(registration: RegistryBuilder.() -> Unit) {
        registrations.add(registration)
    }

    fun build(): FixtureModule = FixtureModule(registrations.toList())
}

fun fixtureModule(block: FixtureModuleBuilder.() -> Unit): FixtureModule = FixtureModuleBuilder().apply(block).build()

inline fun <reified T> FixtureModuleBuilder.register(generator: Generator<T>) {
    addRegistration { register<T>(generator) }
}

inline fun <reified T> FixtureModuleBuilder.register(provider: T) {
    addRegistration { register<T>(provider) }
}

inline fun <reified T> FixtureModuleBuilder.register(noinline factory: RegistrationScope.() -> Generator<T>) {
    addRegistration { register<T>(factory) }
}
