package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PrimitivesTest : FreeSpec({

    "buildRegistry includes primitive generators by default" {
        val registry = buildRegistry {}
        registry.next<String>().shouldBeInstanceOf<String>()
        registry.next<Int>().shouldBeInstanceOf<Int>()
        registry.next<Long>().shouldBeInstanceOf<Long>()
        registry.next<Double>().shouldBeInstanceOf<Double>()
        registry.next<Boolean>().shouldBeInstanceOf<Boolean>()
    }

    "String default generator produces non-empty strings" {
        val registry = buildRegistry {}
        registry.next<String>() shouldNotBe ""
    }

    "user-registered generator overrides default primitive" {
        val registry =
            buildRegistry {
                register<Int>(Generator { 42 })
            }
        registry.next<Int>() shouldBe 42
    }

    "data class auto-generates without explicit registration when primitives are registered" {
        data class Point(val x: Int, val y: Int)
        val registry = buildRegistry {}
        val p = registry.next<Point>()
        p.x.shouldBeInstanceOf<Int>()
        p.y.shouldBeInstanceOf<Int>()
    }
})
