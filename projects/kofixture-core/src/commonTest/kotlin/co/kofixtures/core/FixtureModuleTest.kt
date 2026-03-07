package co.kofixtures.core

import co.kofixtures.core.utils.Person
import co.kofixtures.core.utils.gen
import co.kofixtures.core.utils.random
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class FixtureModuleTest : FreeSpec({

    "can combine multiple modules" {
        val intModule =
            fixtureModule {
                register<Int> { gen { 1 } }
            }
        val stringModule =
            fixtureModule {
                register<String> { gen { "text" } }
            }
        val registry =
            buildRegistry {
                includes(intModule, stringModule)
            }
        registry.generator<Int>().next(random) shouldBe 1
        registry.generator<String>().next(random) shouldBe "text"
    }

    "latest module overrides previous" {
        val oldModule =
            fixtureModule {
                register<Int> { gen { 1 } }
            }
        val newModule =
            fixtureModule {
                register<Int> { gen { 2 } }
            }
        val registry =
            buildRegistry {
                includes(oldModule, newModule)
            }
        registry.generator<Int>().next(random) shouldBe 2
    }

    "can use generators defined in other module during registration" {
        val intModule =
            fixtureModule {
                register<Int> { gen { 1 } }
            }
        val stringModule =
            fixtureModule {
                register<String> {
                    val intGen = registry.generator<Int>()
                    gen { intGen.next(it).toString() }
                }
            }
        val registry =
            buildRegistry {
                includes(intModule, stringModule)
            }
        registry.generator<String>().next(random) shouldBe "1"
    }

    "can combine generators from multiple modules to create new generator" {
        val intModule =
            fixtureModule {
                register<Int> { gen { 20 } }
            }
        val stringModule =
            fixtureModule {
                register<String> { gen { "Joe" } }
                register<Person> {
                    gen {
                        Person(
                            registry.generator<String>().next(it),
                            registry.generator<Int>().next(it),
                        )
                    }
                }
            }
        val registry =
            buildRegistry {
                includes(intModule, stringModule)
            }
        registry.generator<Person>().next(random) shouldBe Person("Joe", 20)
    }

    "collection config is inherited across modules" {
        val collectionsModule =
            fixtureModule {
                collections {
                    mapSize = 1..1
                    listSize = 2..2
                    setSize = 3..3
                }
            }
        val intModule =
            fixtureModule {
                register<Int> { gen { 1 } }
            }
        val registry =
            buildRegistry {
                includes(intModule, collectionsModule)
            }

        registry.generator<Map<Int, Int>>().next(Random.Default) shouldBe mapOf(1 to 1)
        registry.generator<List<Int>>().next(Random.Default) shouldBe listOf(1, 1)
        registry.generator<Set<Int>>().next(Random.Default) shouldBe setOf(1, 1, 1)
    }

    "collection config can be overridden by later module" {
        var counter = 0
        val firstModule =
            fixtureModule {
                collections {
                    mapSize = 1..1
                    listSize = 2..2
                    setSize = 3..3
                }
                register<Int> { gen { counter++ } }
            }
        val secondModule =
            fixtureModule {
                collections {
                    mapSize = 4..4
                    listSize = 5..5
                    setSize = 6..6
                }
            }
        val registry =
            buildRegistry {
                includes(firstModule, secondModule)
            }

        registry.generator<Map<Int, Int>>().next(Random.Default).shouldHaveSize(4)
        registry.generator<List<Int>>().next(Random.Default).shouldHaveSize(5)
        registry.generator<Set<Int>>().next(Random.Default).shouldHaveSize(6)
    }
})
