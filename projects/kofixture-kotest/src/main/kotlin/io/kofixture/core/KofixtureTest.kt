package io.kofixture.core

import io.kofixture.FixtureModule
import io.kofixture.Generator
import io.kofixture.OverrideScope
import io.kofixture.Registry
import io.kofixture.arb
import io.kofixture.generator
import io.kofixture.toArb
import io.kotest.core.test.TestScope
import io.kotest.property.Arb

/**
 * Mixin interface for Kotest specs that use kofixture.
 *
 * Usage:
 * ```kotlin
 * class MyTest : FreeSpec(), KofixtureTest {
 *     init {
 *         "some test" {
 *             val user = next<User>()
 *         }
 *     }
 * }
 * ```
 *
 * The registry is built automatically in `beforeSpec` by [KofixtureExtension].
 * Override [fixtureModules] to register spec-specific modules on top of the defaults.
 */
interface KofixtureTest {
    /** Spec-specific modules. If empty, [KofixtureContext.defaultModules] are used instead. */
    val fixtureModules: List<FixtureModule> get() = emptyList()

    fun registry(): Registry = KofixtureContext.registryFor(this)

    fun buildRegistry() = KofixtureContext.buildFor(this)
}

/** Generates a value of type [T] using the spec's registry. */
inline fun <reified T> KofixtureTest.next(noinline block: OverrideScope.() -> Unit = {}): T = registry().next<T>(block)

/** Returns a [Generator] that produces values of type [T] using the spec's registry. */
inline fun <reified T> KofixtureTest.generator(noinline block: OverrideScope.() -> Unit = {}): Generator<T> =
    Generator { registry().next<T>(block) }

/** Returns an [Arb] that produces values of type [T] using the spec's registry. */
inline fun <reified T> KofixtureTest.arb(noinline block: OverrideScope.() -> Unit = {}): Arb<T> = generator<T>(block).toArb()

// --- Extensions on TestScope for direct use inside test bodies ---
// Works in both constructor-lambda and init{} style specs.

@PublishedApi internal fun TestScope.kofixtureRegistry(): Registry {
    val spec =
        testCase.spec as? KofixtureTest
            ?: error("${testCase.spec::class.simpleName} must implement KofixtureTest to use fixture helpers")
    return spec.registry()
}

/** Generates a value of type [T] directly inside a test body. */
inline fun <reified T> TestScope.next(noinline block: OverrideScope.() -> Unit = {}): T = kofixtureRegistry().next<T>(block)

/** Returns a [Generator] of type [T] directly inside a test body. */
inline fun <reified T> TestScope.generator(noinline block: OverrideScope.() -> Unit = {}): Generator<T> =
    kofixtureRegistry().generator<T>(block)

/** Returns an [Arb] of type [T] directly inside a test body. */
inline fun <reified T> TestScope.arb(noinline block: OverrideScope.() -> Unit = {}): Arb<T> = kofixtureRegistry().arb<T>(block)
