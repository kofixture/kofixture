package co.kofixtures.core

import co.kofixtures.core.utils.gen
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

data class JvmPerson(val name: JvmName, val age: Int, val status: String)

@JvmInline
value class JvmName(val value: String)

class RegistryJvmTest : FreeSpec({

    val registry =
        buildRegistry {
            register<String> { gen { "text" } }
            register<Int> { gen { 42 } }
            registerOf(::JvmName)
            registerOf(::JvmPerson)
        }

    "can resolve auto-created fixture via reflection" {
        registry.generator<JvmPerson>().next(Random) shouldBe JvmPerson(JvmName("text"), 42, "text")
    }

    "can override by type in reflected fixture" {
        registry.sample<JvmPerson> {
            override<String> { "override" }
        } shouldBe JvmPerson(JvmName("override"), 42, "override")
    }

    "can override by property in reflected fixture" {
        registry.sample<JvmPerson> {
            override(JvmPerson::status) { "active" }
        } shouldBe JvmPerson(JvmName("text"), 42, "active")
    }
})
