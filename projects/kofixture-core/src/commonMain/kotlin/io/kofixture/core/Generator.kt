package io.kofixture.core

import kotlin.random.Random

/**
 * A single-method interface that produces values of type [T] from a [Random] source.
 *
 * Implement this interface (or use the functional constructor) to define how a specific
 * type is generated. Generators are composable: one generator may delegate to others
 * via [FixtureRegistry] to build up complex object graphs.
 */
fun interface Generator<T> {
    /**
     * Produces the next value.
     *
     * @param random the source of randomness; pass the same instance through the call tree
     *   to ensure reproducibility when a seed is set.
     */
    fun next(random: Random): T
}
