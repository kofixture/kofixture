package io.kofixture.kotest.arb

import io.kofixture.core.Generator
import io.kofixture.core.buildRegistry
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import kotlin.random.Random

private data class Person(val name: String, val age: Int)

class RegistryTest : FreeSpec({

    val rng = Random(seed = 42)

    "register<T> { Arb<T> }" - {

        "registers an Arb factory as a generator" {
            val registry =
                buildRegistry {
                    register<String> { arbitrary { "from-arb" } }
                }
            registry.generator<String>().next(rng) shouldBe "from-arb"
        }

        "can compose generators using getArb() inside factory" {
            val registry =
                buildRegistry {
                    register<String> { Arb.string(3..3) }
                    register<Int> { Generator { _ -> 10 } }
                    register<Person> {
                        val nameArb = getArb(Person::name)
                        val ageArb = getArb(Person::age)
                        arbitrary { Person(nameArb.bind(), ageArb.bind()) }
                    }
                }
            val person = registry.generator<Person>().next(rng)
            person.name.shouldHaveLength(3)
            person.age shouldBe 10
        }

        "can mix with Generator factory in same registry" {
            val registry =
                buildRegistry {
                    register<String> { arbitrary { "arb-string" } }
                    register<Int> { Generator { _ -> 99 } }
                }
            registry.generator<String>().next(rng) shouldBe "arb-string"
            registry.generator<Int>().next(rng) shouldBe 99
        }
    }

    "FixtureRegistryBuilder.registerArb()" - {

        "registers a bare Arb as a generator" {
            val arb = arbitrary<String> { "bare-arb" }
            val registry =
                buildRegistry {
                    registerArb(arb)
                }
            registry.generator<String>().next(rng) shouldBe "bare-arb"
        }
    }

    "FactoryScope.getArb()" - {

        "returns an Arb that wraps the registered generator" {
            val registry =
                buildRegistry {
                    register<String> { Generator { _ -> "wrapped" } }
                    register<Person> {
                        val nameArb = getArb(Person::name)
                        arbitrary { Person(nameArb.bind(), 0) }
                    }
                }
            registry.generator<Person>().next(rng).name shouldBe "wrapped"
        }
    }
})
