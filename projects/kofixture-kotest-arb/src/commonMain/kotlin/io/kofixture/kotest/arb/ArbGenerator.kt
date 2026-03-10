package io.kofixture.kotest.arb

import io.kofixture.core.FixtureRegistry
import io.kofixture.core.Generator
import io.kofixture.core.OverrideScope
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.random.Random

/**
 * Adapter that wraps a Kotest [Arb] as a [Generator].
 * Enables efficient round-trip: [asArb] on an [ArbGenerator] recovers the original [Arb].
 */
class ArbGenerator<T>(private val arb: Arb<T>) : Generator<T> {
    override fun next(random: Random): T = arb.sample(RandomSource.seeded(random.nextLong())).value

    /** Returns the original [Arb] that was wrapped by this generator. */
    fun unwrap(): Arb<T> = arb
}

/** Converts this [Arb] into a [Generator] via [ArbGenerator]. */
fun <T> Arb<T>.toGenerator(): Generator<T> = ArbGenerator(this)

/**
 * Converts this [Generator] to a Kotest [Arb].
 * If this is already an [ArbGenerator], the original [Arb] is recovered without double-wrapping.
 */
fun <T> Generator<T>.asArb(): Arb<T> = when (this) {
    is ArbGenerator<T> -> unwrap()
    else -> arbitrary { rs -> next(rs.random) }
}

/** Returns an [Arb] for [T] resolved from this registry, applying optional overrides. */
inline fun <reified T> FixtureRegistry.arb(
    tag: String? = null,
    noinline block: OverrideScope<T>.() -> Unit = {},
): Arb<T> = generator<T>(tag, block).asArb()
