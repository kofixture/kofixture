package io.kofixture.kotest.arb

import io.kofixture.core.Generator
import io.kofixture.core.KofixtureTest
import io.kofixture.core.fixtureModule
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary

private val stringModule =
    fixtureModule {
        register<String> { Generator { _ -> "alice" } }
    }

class KofixtureContextTest : FreeSpec(), KofixtureTest {
    override val fixtureModules = listOf(stringModule)

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    init {
        "KofixtureTest.arb() delegate" - {

            "returns an Arb that resolves from the registry" {
                val arb: Arb<String> by arb()
                arb.sample(RandomSource.default()).value shouldBe "alice"
            }

            "supports override block" {
                val overriddenArb = arbitrary<String> { "overridden" }
                val arb: Arb<String> by arb<String> {
                    override<String> { overriddenArb }
                }
                arb.sample(RandomSource.default()).value shouldBe "overridden"
            }
        }
    }
}
