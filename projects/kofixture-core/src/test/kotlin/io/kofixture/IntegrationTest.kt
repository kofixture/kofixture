package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

data class LicenseTrial(val activatedAt: Instant, val activeTill: Instant)

data class Account(val id: String, val license: LicenseTrial)

class IntegrationTest : FreeSpec({

    val module =
        fixtureModule {
            register<Instant>(Generator { Instant.now() })

            register<LicenseTrial> {
                val activatedAtGen = get<Instant>()
                val activeTillGen = get<Instant>()
                Generator {
                    val at = activatedAtGen.next()
                    val till = activeTillGen.next().let { if (it < at) at.plusSeconds(3600) else it }
                    LicenseTrial(at, till)
                }
            }
        }

    val registry = buildRegistry { include(module) }

    "generates LicenseTrial - constraint activeTill >= activatedAt satisfied by generator logic" {
        repeat(10) {
            val trial = registry.next<LicenseTrial>()
            (trial.activeTill >= trial.activatedAt) shouldBe true
        }
    }

    "property override replaces activeTill (constraint bypassed - user responsibility)" {
        val fixed = Instant.parse("2020-01-01T00:00:00Z")
        val trial =
            registry.next<LicenseTrial> {
                override(LicenseTrial::activeTill) with { fixed }
            }
        trial.activeTill shouldBe fixed
    }

    "type override affects all Instant fields during generation" {
        val fixed = Instant.parse("2024-06-01T00:00:00Z")
        val trial =
            registry.next<LicenseTrial> {
                override<Instant> { fixed }
            }
        trial.activatedAt shouldBe fixed
        trial.activeTill shouldBe fixed
    }

    "Account auto-generated - LicenseTrial resolved from registry" {
        val account = registry.next<Account>()
        account.id shouldNotBe ""
        account.license shouldNotBe null
    }

    "nested type override for Instant propagates into LicenseTrial inside Account" {
        val fixed = Instant.parse("2025-01-01T00:00:00Z")
        val account =
            registry.next<Account> {
                override<Instant> { fixed }
            }
        account.license.activatedAt shouldBe fixed
        account.license.activeTill shouldBe fixed
    }
})
