package co.kofixtures.core

import kotlin.random.Random

fun interface Generator<T> {
    fun next(random: Random): T
}
