package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.reflect.typeOf

class GenerationContextTest : FreeSpec({

    "EMPTY has no overrides" {
        GenerationContext.EMPTY.typeOverrides.shouldBeEmpty()
        GenerationContext.EMPTY.propertyOverrides.shouldBeEmpty()
    }

    "contextual generator reads explicit context" {
        val context =
            GenerationContext(
                typeOverrides = mapOf(typeOf<Int>() to Generator { 99 }),
                propertyOverrides = emptyMap(),
            )
        val generator =
            Generator.contextual<Int> { currentContext ->
                @Suppress("UNCHECKED_CAST")
                (currentContext.typeOverrides[typeOf<Int>()] as Generator<Int>).next(currentContext)
            }

        generator.next(context) shouldBe 99
    }

    "parameterless next uses EMPTY context" {
        val generator = Generator.contextual { context -> context.typeOverrides.size }

        generator.next() shouldBe 0
    }
})
