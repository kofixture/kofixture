@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

@file:Suppress("MaxLineLength")

package io.kofixture.ksp

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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class KofixtureProcessorTest : FunSpec({

    test("processor runs without error on @Kofixture object") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture

                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )

        val result = compile(source)

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    test("generates FixtureModule val for annotated object") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Project(val name: String, val description: String)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "val domainFixtures: FixtureModule = fixtureModule {"
        content shouldContain "register<co.example.domain.Project>"
        content shouldContain "sample(co.example.domain.Project::name, random)"
        content shouldContain "sample(co.example.domain.Project::description, random)"
    }

    test("generates enum registration using entries") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                enum class Priority { LOW, MEDIUM, HIGH }
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "register<co.example.domain.Priority>"
        content shouldContain "Priority.entries"
        content shouldContain "random.nextInt("
    }

    test("generates object registration returning the singleton") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                object Singleton
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "register<co.example.domain.Singleton>"
        content shouldContain "Generator { _ -> co.example.domain.Singleton }"
    }

    test("sealed subtypes are registered before the sealed parent") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                sealed class Status {
                    object Active : Status()
                    object Inactive : Status()
                    data class Custom(val value: String) : Status()
                }
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        listOf("Status.Active", "Status.Inactive", "Status.Custom", "Status").forEach { type ->
            content shouldContain "register<co.example.domain.$type>"
        }
        val activeIdx = content.indexOf("register<co.example.domain.Status.Active>")
        val parentIdx = content.indexOf("register<co.example.domain.Status>")
        (activeIdx < parentIdx) shouldBe true
    }

    test("ClassCollector — collects object, enum, data class; skips abstract and nested sealed subtype") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture

                object Singleton
                enum class Color { RED, GREEN }
                data class Point(val x: Int, val y: Int)
                abstract class Base
                interface Marker
                sealed class Shape {
                    data class Circle(val r: Double) : Shape()
                }

                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )

        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "register<co.example.domain.Singleton>"
        content shouldContain "register<co.example.domain.Color>"
        content shouldContain "register<co.example.domain.Point>"
        content shouldContain "register<co.example.domain.Shape>"
        content shouldNotContain "register<co.example.domain.Base>"
        content shouldNotContain "register<co.example.domain.Marker>"
        content shouldNotContain "register<co.example.domain.Circle>"
    }

    test("generates OverrideScope value setter and generator lambda overload for each param") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Person(val name: String, val age: Int)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        // Value setters
        content shouldContain "var OverrideScope<co.example.domain.Person>.name: String?"
        content shouldContain "var OverrideScope<co.example.domain.Person>.age: Int?"
        // Lambda overloads
        content shouldContain
            "fun OverrideScope<co.example.domain.Person>.name(block: OverrideScope<co.example.domain.Person>.() -> Generator<String>)"
        content shouldContain
            "fun OverrideScope<co.example.domain.Person>.age(block: OverrideScope<co.example.domain.Person>.() -> Generator<Int>)"
        // NamedOverrideKey
        content shouldContain "NamedOverrideKey(typeOf<co.example.domain.Person>(), \"name\")"
        content shouldContain "NamedOverrideKey(typeOf<co.example.domain.Person>(), \"age\")"
    }

    test("generates complex-type override overload for complex-type params") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Address(val street: String, val city: String)
                data class User(val name: String, val address: Address)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "class co_example_domain_User__address__m1147692044__FieldScope"
        content shouldContain
            "fun OverrideScope<co.example.domain.User>.address(block: co_example_domain_User__address__m1147692044__FieldScope.() -> Generator<co.example.domain.Address>)"
        content shouldContain
            "fun OverrideScope<co.example.domain.User>.address(block: co_example_domain_User__address__m1147692044__FieldScope.() -> Arb<co.example.domain.Address>)"
        content shouldContain
            "fun OverrideScope<co.example.domain.User>.address(block: co_example_domain_User__address__m1147692044__FieldScope.() -> Unit)"
        content shouldContain "registry.generatorFor<co.example.domain.Address>(typeOf<co.example.domain.Address>()"
        content shouldContain "ActiveOverrides.from(scope.child)"
        content shouldContain "var co_example_domain_User__address__m1147692044__FieldScope.street: String?"
        content shouldContain
            "fun co_example_domain_User__address__m1147692044__FieldScope.street(block: PropOverrideScope<String>.() -> Generator<String>)"
        content shouldContain
            "fun co_example_domain_User__address__m1147692044__FieldScope.street(block: PropOverrideScope<String>.() -> Arb<String>)"
    }

    test("generates distinct field scopes for multiple params of same complex type") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Address(val street: String, val city: String)
                data class User(val home: Address, val work: Address)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "class co_example_domain_User__home__3208415__FieldScope"
        content shouldContain "class co_example_domain_User__work__3655441__FieldScope"
        content shouldContain
            "fun OverrideScope<co.example.domain.User>.home(block: co_example_domain_User__home__3208415__FieldScope.() -> Generator<co.example.domain.Address>)"
        content shouldContain
            "fun OverrideScope<co.example.domain.User>.work(block: co_example_domain_User__work__3655441__FieldScope.() -> Generator<co.example.domain.Address>)"
        content shouldContain "var co_example_domain_User__home__3208415__FieldScope.street: String?"
        content shouldContain "var co_example_domain_User__work__3655441__FieldScope.street: String?"
    }

    test("generates Arb overloads when kotest-property is in classpath") {
        // inheritClassPath = true → io.kotest.property.Arb is on the test classpath → hasArb = true
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Person(val name: String, val age: Int)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )

        val result = compile(source)

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "import io.kotest.property.Arb"
        content shouldContain "import io.kofixture.kotest.arb.ArbGenerator"
        content shouldContain
            "fun OverrideScope<co.example.domain.Person>.name(block: OverrideScope<co.example.domain.Person>.() -> Arb<String>)"
        content shouldContain
            "fun OverrideScope<co.example.domain.Person>.age(block: OverrideScope<co.example.domain.Person>.() -> Arb<Int>)"
        content shouldContain "ArbGenerator(scope.block())"
    }

    test("generates nullable-aware type signatures for nullable params") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Person(val name: String?, val age: Int)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "var OverrideScope<co.example.domain.Person>.name: String?"
        content shouldContain
            "fun OverrideScope<co.example.domain.Person>.name(block: OverrideScope<co.example.domain.Person>.() -> Generator<String?>)"
    }

    test("skips override extensions for private constructor params") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                import java.time.Instant
                data class FixedClock(private val fixedInstant: Instant, val name: String)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldNotContain "OverrideScope<co.example.domain.FixedClock>.fixedInstant"
        content shouldContain "OverrideScope<co.example.domain.FixedClock>.name"
    }

    test("generates @JvmName on value setter to prevent platform declaration clash") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                enum class Status { ACTIVE, INACTIVE }
                data class Person(val status: Status)
                data class Project(val status: Status)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "@get:JvmName(\"co_example_domain_Person__status__get\")"
        content shouldContain "@set:JvmName(\"co_example_domain_Person__status__set\")"
        content shouldContain "@get:JvmName(\"co_example_domain_Project__status__get\")"
        content shouldContain "@set:JvmName(\"co_example_domain_Project__status__set\")"
    }

    test("uses FQN for java.math.BigDecimal in generated code") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                import java.math.BigDecimal
                data class Invoice(val amount: BigDecimal, val tax: BigDecimal)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "java.math.BigDecimal"
        content shouldNotContain "var OverrideScope<co.example.domain.Invoice>.amount: BigDecimal?"
    }

    test("uses FQN for kotlin.time.Instant in generated code") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                import kotlin.time.Instant
                data class FixedClock(val instant: Instant)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "kotlin.time.Instant"
    }

    test("skips generic sealed subtypes in sealed registration") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                sealed interface DomainError
                data class NotFound<T>(val error: String) : DomainError
                data class Unauthorized(val error: String) : DomainError
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "register<co.example.domain.DomainError>"
        content shouldContain "co.example.domain.Unauthorized"
        content shouldNotContain "co.example.domain.NotFound"
    }

    test("skips class with private primary constructor") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                class Singleton private constructor() { companion object { val INSTANCE = Singleton() } }
                data class Person(val name: String)
                @Kofixture(packages = ["co.example.domain"])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldNotContain "register<co.example.domain.Singleton>"
        content shouldContain "register<co.example.domain.Person>"
    }

    test("excludedClasses are omitted from generated module") {
        val source =
            SourceFile.kotlin(
                "Domain.kt",
                """
                package co.example.domain
                import io.kofixture.core.Kofixture
                data class Person(val name: String)
                data class Internal(val id: Int)
                @Kofixture(packages = ["co.example.domain"], excludedClasses = [Internal::class])
                object DomainFixtures
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = result.generatedContent("DomainFixturesGenerated.kt")
        content shouldContain "register<co.example.domain.Person>"
        content shouldNotContain "register<co.example.domain.Internal>"
    }

    test("OverrideScope value override works at runtime") {
        val source =
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

                fun samplePerson(): Person {
                    val registry = buildRegistry {
                        register<String> { Generator { _ -> "default" } }
                        register<Int>    { Generator { _ -> 0 } }
                        includes(domainFixtures)
                    }
                    return registry.sample<Person> { name = "Alice"; age = 42 }
                }
                """.trimIndent(),
            )
        val result = compile(source)
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val person =
            result.classLoader
                .loadClass("co.example.domain.DomainKt")
                .getMethod("samplePerson")
                .invoke(null)
        val nameField = person.javaClass.getDeclaredField("name").apply { isAccessible = true }
        val ageField = person.javaClass.getDeclaredField("age").apply { isAccessible = true }
        nameField.get(person) shouldBe "Alice"
        ageField.get(person) shouldBe 42
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

private fun JvmCompilationResult.generatedContent(fileName: String): String = sourcesGeneratedBySymbolProcessor
    .firstOrNull { it.name == fileName }
    ?.readText()
    ?: error("Generated file not found: $fileName")
