package io.kofixture.examples

import io.kofixture.core.FixtureModule
import io.kofixture.core.Generator
import io.kofixture.core.fixtureModule
import io.kofixture.core.register
import io.kofixture.core.sample
import io.kofixture.kotest.KofixtureFunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val primitives: FixtureModule = fixtureModule {
    register<String> { Generator { random -> "str-${random.nextInt(100_000)}" } }
    register<Int> { Generator { random -> random.nextInt(100_000) } }
}

class ExampleTest : KofixtureFunSpec({
    val user by sample<User> { name = "Alice" }
    val profile by sample<Profile> {
        user { name = "Bob" }
        address { city = "Warsaw" }
    }
    val status by sample<Status>()

    test("overrides name via value setter") {
        user.name shouldBe "Alice"
    }

    test("overrides nested type via scope") {
        profile.user.name shouldBe "Bob"
        profile.address.city shouldBe "Warsaw"
    }

    test("samples enum value") {
        status shouldNotBe null
    }
}) {
    override val fixtureModules: List<FixtureModule> = listOf(primitives, fixtures)
}
