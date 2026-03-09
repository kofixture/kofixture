@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package co.kofixtures.ksp

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KoFixtureIntegrationTest : FunSpec({

    test("samples data class correctly") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import co.kofixtures.core.Generator
                    import co.kofixtures.core.buildRegistry
                    import co.kofixtures.core.register
                    import co.kofixtures.ksp.KoFixture

                    data class Project(val name: String, val description: String)

                    @KoFixture(packages = ["co.example.domain"])
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
                    import co.kofixtures.core.Generator
                    import co.kofixtures.core.buildRegistry
                    import co.kofixtures.core.register
                    import co.kofixtures.ksp.KoFixture

                    data class Person(val name: String, val age: Int)

                    @KoFixture(packages = ["co.example.domain"])
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
                    import co.kofixtures.core.Generator
                    import co.kofixtures.core.buildRegistry
                    import co.kofixtures.core.register
                    import co.kofixtures.ksp.KoFixture

                    data class Address(val street: String, val city: String)
                    data class User(val name: String, val address: Address)

                    @KoFixture(packages = ["co.example.domain"])
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
                    import co.kofixtures.core.buildRegistry
                    import co.kofixtures.ksp.KoFixture

                    sealed class Status { object Active : Status(); object Inactive : Status() }

                    @KoFixture(packages = ["co.example.domain"])
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

    test("enum — all constants produced across many samples") {
        val result =
            compile(
                SourceFile.kotlin(
                    "Domain.kt",
                    """
                    package co.example.domain
                    import co.kofixtures.core.buildRegistry
                    import co.kofixtures.ksp.KoFixture

                    enum class Priority { LOW, MEDIUM, HIGH }

                    @KoFixture(packages = ["co.example.domain"])
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
    c.symbolProcessorProviders = mutableListOf<SymbolProcessorProvider>(KoFixtureProvider())
    c.kspWithCompilation = true
    return c.compile()
}

private fun JvmCompilationResult.invoke(name: String): Any? =
    classLoader.loadClass("co.example.domain.DomainKt").getDeclaredMethod(name).invoke(null)
