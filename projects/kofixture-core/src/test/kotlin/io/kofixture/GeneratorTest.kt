package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class GeneratorTest : FreeSpec({

    "Generator next() returns value from lambda" {
        val gen = Generator { 42 }
        gen.next() shouldBe 42
    }

    "map transforms the generated value" {
        val gen = Generator { 5 }.map { it * 2 }
        gen.next() shouldBe 10
    }

    "filter retries until predicate passes" {
        var count = 0
        val gen =
            Generator {
                count++
                count
            }.filter(predicate = { it >= 3 })
        gen.next() shouldBe 3
    }

    "flatMap chains generators" {
        val gen = Generator { 3 }.flatMap { n -> Generator { "x".repeat(n) } }
        gen.next() shouldBe "xxx"
    }
})
