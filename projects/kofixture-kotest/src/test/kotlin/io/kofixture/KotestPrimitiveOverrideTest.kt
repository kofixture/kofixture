package io.kofixture

import io.kofixture.core.KofixtureContext
import io.kofixture.core.KofixtureTest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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
})
