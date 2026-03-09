package co.kofixtures.kotest.arb

import co.kofixtures.core.Generator
import co.kofixtures.core.buildRegistry
import co.kofixtures.core.register
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.random.Random

class ArbGeneratorTest : FreeSpec({

    val rs = RandomSource.seeded(42L)

    "ArbGenerator" - {

        "next() delegates to the wrapped Arb" {
            val arb = arbitrary<String> { "hello" }
            val gen = ArbGenerator(arb)
            gen.next(rs.random) shouldBe "hello"
        }

        "unwrap() returns the original Arb" {
            val arb = arbitrary<String> { "hello" }
            val gen = ArbGenerator(arb)
            gen.unwrap() shouldBe arb
        }
    }

    "Arb.toGenerator()" - {

        "returns an ArbGenerator wrapping the Arb" {
            val arb = arbitrary<String> { "world" }
            val gen = arb.toGenerator()
            gen.shouldBeInstanceOf<ArbGenerator<String>>()
        }

        "produces values from the Arb" {
            val arb = arbitrary<String> { "world" }
            val gen = arb.toGenerator()
            gen.next(Random.Default) shouldBe "world"
        }
    }

    "Generator.asArb()" - {

        "wraps a plain Generator in an Arb" {
            val gen = Generator<String> { _ -> "plain" }
            val arb = gen.asArb()
            arb.sample(rs).value shouldBe "plain"
        }

        "unwraps ArbGenerator without double-wrapping" {
            val original = arbitrary<String> { "original" }
            val gen = ArbGenerator(original)
            val recovered = gen.asArb()
            recovered shouldBe original
        }
    }

    "FixtureRegistry.arb()" - {

        "creates an Arb that resolves from the registry" {
            val registry =
                buildRegistry {
                    register<String> { Generator { _ -> "fixture" } }
                }
            registry.arb<String>().sample(rs).value shouldBe "fixture"
        }
    }
})
