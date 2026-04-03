package io.kofixture.examples.core

import io.kofixture.Generator
import io.kofixture.buildRegistry
import io.kofixture.fixtureModule
import io.kofixture.register
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

data class EmailAddress(val value: String)

data class CreateAccountRequest(
    val email: EmailAddress,
    val password: String,
    val locale: String,
)

class CoreOnlyExampleTest : FreeSpec({

    "buildRegistry generates a valid request and supports scoped overrides" {
        val accountModule =
            fixtureModule {
                register<EmailAddress>(Generator { EmailAddress("john@example.com") })
                register<String>(Generator { "generated-value" })
            }

        val registry =
            buildRegistry {
                include(accountModule)
            }

        val request =
            registry.next<CreateAccountRequest> {
                override(CreateAccountRequest::password) with "strong-password-123"
            }

        request.email shouldBe EmailAddress("john@example.com")
        request.password shouldBe "strong-password-123"
        request.locale shouldBe "generated-value"
    }
})
