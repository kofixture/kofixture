package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

sealed class Shape {
    data class Circle(val radius: Double) : Shape()

    data class Rectangle(val width: Double, val height: Double) : Shape()
}

class SealedClassTest : FreeSpec({

    "sealed class auto-generates a random subtype" {
        val registry = buildRegistry {}
        // 50 samples: P(missing any subtype with equal probability) ≈ 10^-14
        val shapes = (1..50).map { registry.next<Shape>() }
        shapes.any { it is Shape.Circle } shouldBe true
        shapes.any { it is Shape.Rectangle } shouldBe true
    }

    "property override works on a concrete sealed subtype" {
        val registry = buildRegistry {}
        repeat(10) {
            val circle =
                registry.next<Shape.Circle> {
                    override(Shape.Circle::radius) with { 99.9 }
                }
            circle.radius shouldBe 99.9
        }
    }
})
