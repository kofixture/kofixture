package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe

class CollectionsTest : FreeSpec({

    "List<T> is auto-generated with default size" {
        val registry = buildRegistry { register<String>(Generator { "x" }) }
        val result = registry.next<List<String>>()
        result.size shouldBeInRange 1..5
        result.size shouldBeInRange 1..5
    }

    "Set<T> is auto-generated with default size" {
        val registry = buildRegistry { register<String>(Generator { "x" }) }
        val result = registry.next<Set<String>>()
        result.size shouldBeInRange 1..5
    }

    "Map<K, V> is auto-generated with default size" {
        val registry =
            buildRegistry {
                register<String>(Generator { "k" })
                register<Int>(Generator { 1 })
            }
        val result = registry.next<Map<String, Int>>()
        result.size shouldBeInRange 1..5
    }

    "collections { list = } overrides List size" {
        val registry = buildRegistry { register<String>(Generator { "x" }) }
        val result = registry.next<List<String>> { collections { list = 3..3 } }
        result.size shouldBe 3
    }

    "collections { set = } overrides Set size" {
        var counter = 0
        val registry = buildRegistry { register<String>(Generator { "item-${counter++}" }) }
        val result = registry.next<Set<String>> { collections { set = 4..4 } }
        result.size shouldBe 4
    }

    "collections { map = } overrides Map size" {
        var counter = 0
        val registry =
            buildRegistry {
                register<String>(Generator { "key-${counter++}" })
                register<Int>(Generator { counter++ })
            }
        val result = registry.next<Map<String, Int>> { collections { map = 2..2 } }
        result.size shouldBe 2
    }

    "List field in data class is auto-generated" {
        data class Container(val items: List<String>)
        val registry = buildRegistry { register<String>(Generator { "item" }) }
        val result = registry.next<Container>()
        result.items.size shouldBeInRange 1..5
    }

    "Set field in data class is auto-generated" {
        data class Container(val tags: Set<String>)
        var counter = 0
        val registry = buildRegistry { register<String>(Generator { "tag-${counter++}" }) }
        val result = registry.next<Container>()
        result.tags.size shouldBeInRange 1..5
    }

    "Map field in data class is auto-generated" {
        data class Container(val mapping: Map<String, Int>)
        var counter = 0
        val registry =
            buildRegistry {
                register<String>(Generator { "key-${counter++}" })
                register<Int>(Generator { counter++ })
            }
        val result = registry.next<Container>()
        result.mapping.size shouldBeInRange 1..5
    }

    "explicitly registered Generator<List<T>> takes priority over auto-generation" {
        val registry =
            buildRegistry {
                register<List<String>>(Generator { listOf("explicit") })
            }
        val result = registry.next<List<String>>()
        result shouldBe listOf("explicit")
    }
})
