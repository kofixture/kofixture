package co.kofixtures.core

import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.typeOf

/**
 * Mixin interface for Kotest specs.
 * Provides property delegates with Kotest lifecycle integration.
 *
 * Usage:
 * ```kotlin
 * class MySpec : FreeSpec(), KofixtureTest {
 *     override val fixtureModules = listOf(myModule)
 *
 *     override suspend fun beforeSpec(spec: Spec) {
 *         super.beforeSpec(spec)
 *         buildRegistry()
 *     }
 *
 *     val user: User by sample()
 *     val userGen: Generator<User> by generator()
 * }
 * ```
 */
interface KofixtureTest {
    val fixtureModules: List<FixtureModule> get() = emptyList()

    fun registry(): FixtureRegistry = KofixtureContext.registryFor(this)

    fun buildRegistry() = KofixtureContext.buildFor(this)
}

inline fun <reified T> KofixtureTest.sample(
    tag: String? = null,
    random: Random = Random,
    noinline block: OverrideScope<T>.() -> Unit = {},
): ReadOnlyProperty<Any?, T> {
    val spec = this
    return ReadOnlyProperty { _, _ ->
        KofixtureContext.registryFor(spec).generator<T>(tag, block).next(random)
    }
}

inline fun <reified T> KofixtureTest.generator(
    tag: String? = null,
    noinline block: OverrideScope<T>.() -> Unit = {},
): ReadOnlyProperty<Any?, Generator<T>> {
    val spec = this
    return ReadOnlyProperty { _, _ ->
        KofixtureContext.registryFor(spec).generator<T>(tag, block)
    }
}
