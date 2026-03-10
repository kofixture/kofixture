package io.kofixture.kotest.arb

import io.kofixture.core.KofixtureContext
import io.kofixture.core.KofixtureTest
import io.kofixture.core.OverrideScope
import io.kotest.property.Arb
import kotlin.properties.ReadOnlyProperty

/**
 * Property delegate returning an [Arb] resolved from this spec's registry.
 *
 * Usage:
 * ```kotlin
 * val names: Arb<String> by arb()
 * val user: Arb<User> by arb { override(User::name) { "fixed" } }
 * ```
 */
inline fun <reified T> KofixtureTest.arb(
    tag: String? = null,
    noinline block: OverrideScope<T>.() -> Unit = {},
): ReadOnlyProperty<Any?, Arb<T>> {
    val spec = this
    return ReadOnlyProperty { _, _ ->
        KofixtureContext.registryFor(spec).arb<T>(tag, block)
    }
}
