package io.kofixture

import io.kofixture.core.KofixtureContext
import io.kofixture.core.KofixtureTest
import io.kofixture.core.arb
import io.kofixture.core.generator
import io.kofixture.core.next
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.RandomSource

class KotestPrimitiveOverrideTest : FreeSpec({

    "kotestPrimitivesModule can override core primitive generators explicitly" {
        val registry =
            buildRegistry {
                include(kotestPrimitivesModule)
                register<String>(Generator { "manual-user-override" })
            }

        registry.next<String>() shouldBe "manual-user-override"
    }

    "KofixtureContext adds kotest primitive overrides for Kofixture specs" {
        val module =
            fixtureModule {
                register<String>(Generator { "from-spec-module" })
            }
        val spec =
            object : KofixtureTest {
                override val fixtureModules = listOf(module)
            }

        KofixtureContext.buildFor(spec)
        try {
            spec.registry().next<String>() shouldBe "from-spec-module"
        } finally {
            KofixtureContext.releaseFor(spec)
        }
    }

    "Kofixture specs can use next, generator, and arb during spec construction" {
        val module =
            fixtureModule {
                register<String>(Generator { "from-spec-module" })
            }
        val previousDefaultModules = KofixtureContext.defaultModules
        KofixtureContext.defaultModules = listOf(module)

        try {
            val spec =
                object : KofixtureFreeSpec({
                    val eagerNext = next<String>()
                    val eagerGenerator = generator<String>()
                    val eagerArb = arb<String>()

                    "uses eagerly created helpers" {
                        eagerNext shouldBe "from-spec-module"
                        eagerGenerator.next() shouldBe "from-spec-module"
                        eagerArb.sample(RandomSource.default()).value shouldBe "from-spec-module"
                    }
                }) {}

            spec.registry().next<String>() shouldBe "from-spec-module"
            KofixtureContext.releaseFor(spec)
        } finally {
            KofixtureContext.defaultModules = previousDefaultModules
        }
    }
})
