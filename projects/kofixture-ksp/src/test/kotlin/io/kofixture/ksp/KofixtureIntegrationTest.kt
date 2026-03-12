@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.kofixture.ksp

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KofixtureIntegrationTest : FunSpec({

    test("samples data class correctly") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.Generator
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.register
                    import io.kofixture.core.Kofixture

                    data class Project(val name: String, val description: String)

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun sampleProject(): Any = buildRegistry {
                        register<String> { Generator { _ -> "test-value" } }
                        includes(domainFixtures)
                    }.sample<Project>()
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val project = result.invoke("sampleProject")!!
        project.javaClass
            .getDeclaredField("name")
            .apply { isAccessible = true }
            .get(project) shouldBe "test-value"
    }

    test("OverrideScope value setter overrides field correctly") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.Generator
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.register
                    import io.kofixture.core.Kofixture

                    data class Person(val name: String, val age: Int)

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun samplePerson(): Any = buildRegistry {
                        register<String> { Generator { _ -> "default" } }
                        register<Int>    { Generator { _ -> 0 } }
                        includes(domainFixtures)
                    }.sample<Person> { name = "Alice"; age = 42 }
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val person = result.invoke("samplePerson")!!
        person.javaClass
            .getDeclaredField("name")
            .apply { isAccessible = true }
            .get(person) shouldBe "Alice"
        person.javaClass
            .getDeclaredField("age")
            .apply { isAccessible = true }
            .get(person) shouldBe 42
    }

    test("nested scope override sets only the targeted field") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.Generator
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.register
                    import io.kofixture.core.Kofixture

                    data class Address(val street: String, val city: String)
                    data class User(val name: String, val address: Address)

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun sampleUser(): Any = buildRegistry {
                        register<String> { Generator { _ -> "default" } }
                        includes(domainFixtures)
                    }.sample<User> { address { street = "Main St" } }
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val user = result.invoke("sampleUser")!!
        val address =
            user.javaClass
                .getDeclaredField("address")
                .apply { isAccessible = true }
                .get(user)!!
        address.javaClass
            .getDeclaredField("street")
            .apply { isAccessible = true }
            .get(address) shouldBe "Main St"
    }

    test("sealed class — all subtypes produced across many samples") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.Kofixture

                    sealed class Status { object Active : Status(); object Inactive : Status() }

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun sampleStatus(): Any = buildRegistry { includes(domainFixtures) }.sample<Status>()
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val sampleFn =
            result.classLoader
                .loadClass("co.example.domain.DomainKt")
                .getDeclaredMethod("sampleStatus")
        val seen = mutableSetOf<String>()
        repeat(40) { seen.add(sampleFn.invoke(null)!!.javaClass.simpleName) }
        seen shouldBe setOf("Active", "Inactive")
    }

    test("Arb lambda overload works at runtime") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.Generator
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.register
                    import io.kofixture.core.Kofixture
                    import io.kotest.property.Arb
                    import io.kotest.property.arbitrary.constant

                    data class Person(val name: String)

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun samplePerson(): Any = buildRegistry {
                        register<String> { Generator { _ -> "default" } }
                        includes(domainFixtures)
                    }.sample<Person> { name { Arb.constant("Bob") } }
                    """.trimIndent(),
                ),
            )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val person = result.invoke("samplePerson")!!
        person.javaClass
            .getDeclaredField("name")
            .apply { isAccessible = true }
            .get(person) shouldBe "Bob"
    }

    test("Arb lambda override works for complex types") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.Generator
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.register
                    import io.kofixture.core.Kofixture
                    import io.kotest.property.arbitrary.arbitrary

                    data class Address(val street: String)
                    data class User(val name: String, val address: Address)
                    data class Assignment(val user: User)

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun sampleAssignment(): Any = buildRegistry {
                        register<String> { Generator { _ -> "default" } }
                        includes(domainFixtures)
                    }.sample<Assignment> {
                        user { arbitrary { User("Joe", Address("Main St")) } }
                    }
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val assignment = result.invoke("sampleAssignment")!!
        val user =
            assignment.javaClass
                .getDeclaredField("user")
                .apply { isAccessible = true }
                .get(assignment)
        user.javaClass
            .getDeclaredField("name")
            .apply { isAccessible = true }
            .get(user) shouldBe "Joe"
    }

    test("enum — all constants produced across many samples") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import io.kofixture.core.buildRegistry
                    import io.kofixture.core.Kofixture

                    enum class Priority { LOW, MEDIUM, HIGH }

                    @Kofixture(packages = ["co.example.domain"])
                    object DomainFixtures

                    fun samplePriority(): Any = buildRegistry { includes(domainFixtures) }.sample<Priority>()
                    """.trimIndent(),
                ),
            )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val sampleFn =
            result.classLoader
                .loadClass("co.example.domain.DomainKt")
                .getDeclaredMethod("samplePriority")
        val seen = mutableSetOf<String>()
        repeat(60) { seen.add((sampleFn.invoke(null) as Enum<*>).name) }
        seen shouldBe setOf("LOW", "MEDIUM", "HIGH")
    }
})

private fun compile(vararg sources: SourceFile): JvmCompilationResult {
    val c = KotlinCompilation()
    c.sources = sources.toList()
    c.inheritClassPath = true
    c.jvmTarget = "21"
    c.configureKsp {}
    c.symbolProcessorProviders = mutableListOf<SymbolProcessorProvider>(KofixtureProvider())
    c.kspWithCompilation = true
    return c.compile()
}

private fun JvmCompilationResult.invoke(name: String): Any? =
    classLoader.loadClass("co.example.domain.DomainKt").getDeclaredMethod(name).invoke(null)
