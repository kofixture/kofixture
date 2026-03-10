package io.kofixture.core

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
    /**
     * Fixture modules this test spec uses to build its registry.
     *
     * When the list is empty, [KofixtureContext.defaultModules] is used instead. Override this
     * property in a spec subclass to supply modules without a constructor parameter.
     */
    val fixtureModules: List<FixtureModule> get() = emptyList()

    /**
     * Returns the [FixtureRegistry] that was built for this spec during `beforeSpec`.
     *
     * Throws if [buildRegistry] has not been called yet.
     */
    fun registry(): FixtureRegistry = KofixtureContext.registryFor(this)

    /**
     * Initialises the registry for this spec by applying [fixtureModules] (or
     * [KofixtureContext.defaultModules] if empty). Called automatically by spec base classes
     * and `KofixtureListener` in `beforeSpec`; invoke manually only when inheriting
     * [KofixtureTest] directly without a provided base class.
     */
    fun buildRegistry() = KofixtureContext.buildFor(this)
}

/**
 * Delegate that resolves a new [T] sample on every property access.
 *
 * The [block] applies per-access overrides; it runs inside an [OverrideScope] so KSP-generated
 * extension properties (e.g. `name = "Alice"`) are available when KSP is used.
 */
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

/**
 * Delegate that resolves the [Generator] for [T] on every property access.
 *
 * Useful when the generator itself (rather than a single value) is needed, for example to draw
 * multiple independent samples or to pass to a property-based testing framework.
 */
inline fun <reified T> KofixtureTest.generator(
    tag: String? = null,
    noinline block: OverrideScope<T>.() -> Unit = {},
): ReadOnlyProperty<Any?, Generator<T>> {
    val spec = this
    return ReadOnlyProperty { _, _ ->
        KofixtureContext.registryFor(spec).generator<T>(tag, block)
    }
}
