package co.kofixtures.core

import co.kofixtures.core.utils.Person
import co.kofixtures.core.utils.gen
import co.kofixtures.core.utils.random
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RegistryTest : FreeSpec({

    "resolve" - {

        "returns registered generator" {
            val registry =
                buildRegistry {
                    register<String> { gen("hello") }
                }
            registry.generator<String>().next(random) shouldBe "hello"
        }

        "throws when type is not registered" {
            val registry = buildRegistry {}
            shouldThrow<IllegalStateException> {
                registry.generator<String>().next(random)
            }
        }

        "error message contains the missing type name" {
            val registry = buildRegistry {}
            val error =
                shouldThrow<IllegalStateException> {
                    registry.generator<String>().next(random)
                }
            error.message!!.contains("kotlin.String") shouldBe true
        }

        "factory can resolve other generators from registry" {
            val registry =
                buildRegistry {
                    register<String> { gen("world") }
                    register<String>(tag = "greeting") {
                        val base = registry.generator<String>()
                        gen { rng -> "hello ${base.next(rng)}" }
                    }
                }
            registry.generator<String>(tag = "greeting").next(random) shouldBe "hello world"
        }
    }

    "tags" - {

        "resolve with tag returns the correct generator" {
            val registry =
                buildRegistry {
                    register<String> { gen("primary") }
                    register<String>(tag = "short") { gen("x") }
                }
            registry.generator<String>().next(random) shouldBe "primary"
            registry.generator<String>(tag = "short").next(random) shouldBe "x"
        }

        "primary acts as fallback when tag is not found" {
            val registry =
                buildRegistry {
                    register<String> { gen("fallback") }
                }
            registry.generator<String>(tag = "email").next(random) shouldBe "fallback"
        }

        "throws when neither the tag nor primary is registered" {
            val registry =
                buildRegistry {
                    register<String>(tag = "short") { gen("x") }
                }
            shouldThrow<IllegalStateException> {
                registry.generator<String>(tag = "email").next(random)
            }
        }

        "error message contains the tag" {
            val registry = buildRegistry {}
            val error =
                shouldThrow<IllegalStateException> {
                    registry.generator<String>(tag = "email").next(random)
                }
            error.message!!.contains("tag=\"email\"") shouldBe true
        }

        "type-based scope override takes priority over tagged registry" {
            val registry =
                buildRegistry {
                    register<String> { gen("primary") }
                    register<String>(tag = "short") { gen("x") }
                }
            // override<String> intercepts all String resolutions in this scope, including tagged ones
            registry
                .generator<String>(tag = "short") {
                    override<String> { "overridden" }
                }.next(random) shouldBe "overridden"
        }
    }

    "nullable derivation" - {

        "String? is automatically derived from String" {
            val registry =
                buildRegistry {
                    register<String> { gen("hello") }
                }
            val results = List(100) { registry.generator<String?>().next(random) }
            results.any { it == null } shouldBe true
            results.any { it == "hello" } shouldBe true
        }

        "throws when non-null type is not registered" {
            val registry = buildRegistry {}
            shouldThrow<IllegalStateException> {
                registry.generator<String?>().next(random)
            }
        }

        "explicitly registered T? takes priority over auto-derivation" {
            val registry =
                buildRegistry {
                    register<String> { gen("non-null") }
                    register<String?> { gen("explicit-nullable") }
                }
            registry.generator<String?>().next(random) shouldBe "explicit-nullable"
        }

        "nullable with tag is derived from tagged non-null" {
            val registry =
                buildRegistry {
                    register<String>(tag = "short") { gen("x") }
                }
            val results = List(20) { registry.generator<String?>(tag = "short").next(random) }
            results.any { it == null } shouldBe true
            results.any { it == "x" } shouldBe true
        }

        "no tag nullable falls back to non-tagged non-null" {
            val registry =
                buildRegistry {
                    register<String> { gen("x") }
                }
            val results = List(20) { registry.generator<String?>("short").next(random) }
            results.any { it == null } shouldBe true
            results.any { it == "x" } shouldBe true
        }
    }

    "collections" - {

        "List<T> is auto-derived from T" {
            val registry =
                buildRegistry {
                    register<String> { gen("item") }
                }
            val list = registry.generator<List<String>>().next(random)
            list.shouldBeInstanceOf<List<String>>()
            list.all { it == "item" } shouldBe true
        }

        "Set<T> is auto-derived from T" {
            val registry =
                buildRegistry {
                    register<String> { gen("item") }
                }
            val set = registry.generator<Set<String>>().next(random)
            set.shouldBeInstanceOf<Set<String>>()
            set.all { it == "item" } shouldBe true
        }

        "Map<K,V> is auto-derived from K and V" {
            val registry =
                buildRegistry {
                    register<String> { gen("key") }
                    register<Int> { gen(42) }
                }
            val map = registry.generator<Map<String, Int>>().next(random)
            map.shouldBeInstanceOf<Map<String, Int>>()
            map.keys.all { it == "key" } shouldBe true
            map.values.all { it == 42 } shouldBe true
        }

        "Map<K, Collection<V>> is auto-derived from K and V" {
            val registry =
                buildRegistry {
                    register<String> { gen("key") }
                    register<Int> { gen(42) }
                }
            val map = registry.generator<Map<String, Collection<Int>>>().next(random)
            map.shouldBeInstanceOf<Map<String, Collection<Int>>>()
            map.keys.all { it == "key" } shouldBe true
            map.values.all { c -> c.all { it == 42 } } shouldBe true
        }

        "List<T> throws when T is not registered" {
            val registry = buildRegistry {}
            shouldThrow<IllegalStateException> {
                registry.generator<List<String>>().next(random)
            }
        }

        "List<String?> works when String is registered" {
            val registry =
                buildRegistry {
                    register<String> { gen("item") }
                }
            registry
                .generator<List<String?>>()
                .next(random)
                .shouldBeInstanceOf<List<String?>>()
        }
    }

    "buildRegistry" - {

        "empty registry is valid" {
            buildRegistry {}.shouldNotBeNull()
        }

        "last registration wins when same type is registered twice" {
            val registry =
                buildRegistry {
                    register<String> { gen("first") }
                    register<String> { gen("second") }
                }
            registry.generator<String>().next(random) shouldBe "second"
        }
    }

    "collectionConfig" - {

        "default ranges are 1..5 for all types" {
            val registry =
                buildRegistry {
                    register<String> { gen("item") }
                    register<Int> { gen(0) }
                }
            val results = List(50) { registry.generator<List<String>>().next(random).size }
            results.all { it in 1..5 } shouldBe true
        }

        "listSize is respected" {
            val registry =
                buildRegistry {
                    collections { listSize = 10..10 }
                    register<String> { gen("item") }
                }
            registry.generator<List<String>>().next(random).size shouldBe 10
        }

        "setSize is respected" {
            val registry =
                buildRegistry {
                    collections { setSize = 2..2 }
                    register<Int> { gen { rng -> rng.nextInt() } }
                }
            registry.generator<Set<Int>>().next(random).size shouldBe 2
        }

        "mapSize is respected" {
            val registry =
                buildRegistry {
                    collections { mapSize = 3..3 }
                    register<String> { gen { rng -> rng.nextInt().toString() } }
                    register<Int> { gen(0) }
                }
            registry.generator<Map<String, Int>>().next(random).size shouldBe 3
        }
    }

    "factoryScope" - {

        "can access other generators during registration" {
            val registry =
                buildRegistry {
                    register<Int> { gen { 1 } }
                    register<String> {
                        val intGen = registry.generator<Int>()
                        gen { intGen.next(it).toString() }
                    }
                }
            registry.generator<String>().next(random) shouldBe "1"
        }

        "can get sample object during registration" {
            val registry =
                buildRegistry {
                    register<Int> { gen { 20 } }
                    register<String> { gen { "Joe" } }
                    register<Person> {
                        gen {
                            Person(
                                registry.sample<String>(it),
                                registry.sample<Int>(it),
                            )
                        }
                    }
                }
            registry.generator<Person>().next(random) shouldBe Person("Joe", 20)
        }
    }
})
