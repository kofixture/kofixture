package io.kofixture.examples.kotest

import io.kofixture.Generator
import io.kofixture.KofixtureStringSpec
import io.kofixture.arb
import io.kofixture.fixtureModule
import io.kofixture.register
import io.kofixture.core.next
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant

data class SignupRequest(
    val email: String,
    val password: String,
)

private val authModule =
    fixtureModule {
        register<String> { Arb.constant("generated@example.com") }
        register<SignupRequest> {
            Generator {
                SignupRequest(
                    email = "generated@example.com",
                    password = "correct-horse-battery-staple",
                )
            }
        }
    }

class KotestExampleTest : KofixtureStringSpec() {
    override val fixtureModules = listOf(authModule)

    init {
        "supports next<T>() and property overrides inside kotest specs" {
            val request =
                next<SignupRequest> {
                    override(SignupRequest::email) with "invalid"
                }

            request.email shouldBe "invalid"
        }

        "supports Registry.arb() through the kotest integration module" {
            registry().arb<String>().sample(RandomSource.default()).value shouldBe "generated@example.com"
        }
    }
}
