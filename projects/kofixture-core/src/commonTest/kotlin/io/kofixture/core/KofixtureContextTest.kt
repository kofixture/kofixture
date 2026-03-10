package io.kofixture.core

import io.kofixture.core.utils.gen
import io.kofixture.core.utils.random
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

private val stringModule =
    fixtureModule {
        register<String> { gen { "text" } }
    }

class KofixtureContextTest : FreeSpec(), KofixtureTest {
    override val fixtureModules = listOf(stringModule)

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    init {
        "should generate string" {
            registry().sample<String>() shouldBe "text"
        }

        "should generate value using sample delegate" {
            val string: String by sample()
            string shouldBe "text"
        }

        "should get generator using delegate" {
            val stringGenerator: Generator<String> by generator()
            stringGenerator.next(random) shouldBe "text"
        }
    }
}
