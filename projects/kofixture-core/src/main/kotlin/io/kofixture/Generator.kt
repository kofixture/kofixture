package io.kofixture

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary

/**
 * A functional interface that produces values of type [T] on demand.
 *
 * Create with a lambda: `Generator { 42 }` or `Generator { random.nextInt() }`
 */
class Generator<T> private constructor(
    private val generate: (GenerationContext) -> T,
) {
    constructor(generate: () -> T) : this({ _: GenerationContext -> generate() })

    fun next(): T = next(GenerationContext.EMPTY)

    @PublishedApi
    internal fun next(context: GenerationContext): T = generate(context)

    companion object {
        @PublishedApi
        internal fun <T> contextual(generate: (GenerationContext) -> T): Generator<T> = Generator(generate)
    }
}

fun <T, R> Generator<T>.map(transform: (T) -> R): Generator<R> = Generator.contextual { context ->
    transform(next(context))
}

/**
 * Returns a [Generator] that retries until [predicate] passes.
 *
 * @throws IllegalStateException if the predicate is not satisfied within [maxAttempts] calls.
 */
fun <T> Generator<T>.filter(
    maxAttempts: Int = 1_000,
    predicate: (T) -> Boolean,
): Generator<T> = Generator.contextual { context ->
    var attempts = 0
    var value = next(context)
    while (!predicate(value)) {
        if (++attempts >= maxAttempts) {
            error("Generator.filter: predicate not satisfied after $maxAttempts attempts")
        }
        value = next(context)
    }
    value
}

fun <T, R> Generator<T>.flatMap(transform: (T) -> Generator<R>): Generator<R> = Generator.contextual { context ->
    transform(next(context)).next(context)
}

fun <T> Generator<T>.toArb(): Arb<T> = arbitrary { next() }

/**
 * Wraps this [Arb] as a [Generator]. A fresh [RandomSource] is created on every
 * [Generator.next] call, making each invocation independent and thread-safe.
 */
fun <T> Arb<T>.toGenerator(): Generator<T> = Generator { sample(RandomSource.default()).value }
