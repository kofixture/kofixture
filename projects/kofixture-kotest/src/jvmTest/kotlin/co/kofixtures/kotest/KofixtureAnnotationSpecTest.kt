package co.kofixtures.kotest

import co.kofixtures.core.Generator
import co.kofixtures.core.fixtureModule
import co.kofixtures.core.register
import co.kofixtures.core.sample
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val annotationModule =
    fixtureModule {
        register<String> { Generator { _ -> "annotated" } }
    }

class KofixtureAnnotationSpecTest : FunSpec({

    test("buildRegistry is called in beforeSpec") {
        val spec =
            object : KofixtureAnnotationSpec() {
                override val fixtureModules = listOf(annotationModule)
            }
        spec.beforeSpec(spec)
        shouldNotThrowAny { spec.registry() }
    }

    test("releaseFor is called in afterSpec") {
        val spec =
            object : KofixtureAnnotationSpec() {
                override val fixtureModules = listOf(annotationModule)
            }
        spec.beforeSpec(spec)
        spec.afterSpec(spec)
        shouldThrow<IllegalStateException> { spec.registry() }
    }

    test("sample delegate resolves after buildRegistry") {
        val spec =
            object : KofixtureAnnotationSpec() {
                override val fixtureModules = listOf(annotationModule)
                val value by sample<String>()
            }
        spec.beforeSpec(spec)
        spec.value shouldBe "annotated"
    }
})
