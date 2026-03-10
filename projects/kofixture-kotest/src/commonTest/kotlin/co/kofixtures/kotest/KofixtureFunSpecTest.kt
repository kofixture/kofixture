package co.kofixtures.kotest

import co.kofixtures.core.FixtureModule
import co.kofixtures.core.Generator
import co.kofixtures.core.fixtureModule
import co.kofixtures.core.register
import co.kofixtures.core.sample
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val module: FixtureModule =
    fixtureModule {
        register<String> { Generator { _ -> "hello" } }
    }

class KofixtureFunSpecTest : FunSpec({

    test("buildRegistry is called in beforeSpec") {
        val spec =
            object : KofixtureFunSpec() {
                override val fixtureModules = listOf(module)
            }
        spec.beforeSpec(spec)
        shouldNotThrowAny { spec.registry() }
    }

    test("releaseFor is called in afterSpec") {
        val spec =
            object : KofixtureFunSpec() {
                override val fixtureModules = listOf(module)
            }
        spec.beforeSpec(spec)
        spec.afterSpec(spec)
        shouldThrow<IllegalStateException> { spec.registry() }
    }

    test("sample delegate resolves after buildRegistry") {
        val spec =
            object : KofixtureFunSpec() {
                override val fixtureModules = listOf(module)
                val value by sample<String>()
            }
        spec.beforeSpec(spec)
        spec.value shouldBe "hello"
    }
})
