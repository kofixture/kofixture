package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant

class KotestArbSupportTest : FreeSpec({

    "Generator can be converted to Arb" {
        Generator { "hello" }.toArb().sample(RandomSource.default()).value shouldBe "hello"
    }

    "Arb can be converted to Generator" {
        Arb.constant("hello").toGenerator().next() shouldBe "hello"
    }

    "Registry.arb delegates to the registry generator" {
        val registry =
            buildRegistry {
                register<String>(Generator { "hello" })
            }

        registry.arb<String>().sample(RandomSource.default()).value shouldBe "hello"
    }

    "RegistryBuilder register accepts Arb factories through kotest integration" {
        val registry =
            buildRegistry {
                register<String> { Arb.constant("hello") }
            }

        registry.next<String>() shouldBe "hello"
    }

    "FixtureModuleBuilder register accepts Arb factories through kotest integration" {
        val module =
            fixtureModule {
                register<String> { Arb.constant("hello") }
            }
        val registry =
            buildRegistry {
                include(module)
            }

        registry.next<String>() shouldBe "hello"
    }
})
