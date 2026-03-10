package io.kofixture.core.utils

import io.kofixture.core.Generator
import kotlin.random.Random

val random = Random(seed = 42)

fun <T> gen(value: T) = Generator<T> { _ -> value }

fun <T> gen(block: (Random) -> T) = Generator(block)

data class Person(
    val name: String,
    val age: Int,
)

data class Project(
    val name: String,
    val description: String,
)

data class ProjectAssignment(
    val project: Project,
    val user: Person,
)

data class Team(val members: List<Person>)
