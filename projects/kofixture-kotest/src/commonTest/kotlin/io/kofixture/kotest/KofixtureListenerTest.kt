package io.kofixture.kotest

import io.kofixture.core.FixtureModule
import io.kofixture.core.Generator
import io.kofixture.core.KofixtureTest
import io.kofixture.core.fixtureModule
import io.kofixture.core.register
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe

private val module: FixtureModule =
    fixtureModule {
        register<String> { Generator { _ -> "test" } }
    }

class KofixtureListenerTest : FunSpec({

    test("builds registry for KofixtureTest spec in beforeSpec") {
        val spec =
            object : KofixtureTest, FunSpec() {
                override val fixtureModules = listOf(module)
            }
        val listener = KofixtureListener()
        listener.beforeSpec(spec)
        spec.registry() shouldNotBe null
    }

    test("releases registry for KofixtureTest spec in afterSpec") {
        val spec =
            object : KofixtureTest, FunSpec() {
                override val fixtureModules = listOf(module)
            }
        val listener = KofixtureListener()
        listener.beforeSpec(spec)
        listener.afterSpec(spec)
        shouldThrow<IllegalStateException> { spec.registry() }
    }

    test("skips non-KofixtureTest spec in beforeSpec without error") {
        val spec = object : FunSpec() {}
        val listener = KofixtureListener()
        shouldNotThrowAny { listener.beforeSpec(spec) }
    }

    test("skips non-KofixtureTest spec in afterSpec without error") {
        val spec = object : FunSpec() {}
        val listener = KofixtureListener()
        shouldNotThrowAny { listener.afterSpec(spec) }
    }
})
