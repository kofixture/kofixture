package io.kofixture

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.random.nextInt

@JvmInline value class UserId(val raw: String)

class RegistryTest : FreeSpec({

    "next<T>() returns value from directly registered Generator" {
        val registry =
            buildRegistry {
                register<String>(Generator { "hello" })
            }
        registry.next<String>() shouldBe "hello"
    }

    "next<T>() for unregistered data class auto-generates via primaryConstructor" {
        data class Point(val x: Int, val y: Int)
        val registry =
            buildRegistry {
                register<Int>(Generator { 7 })
            }
        val point = registry.next<Point>()
        point.x shouldBe 7
        point.y shouldBe 7
    }

    "property override replaces specific field post-construction" {
        data class User(val name: String, val age: Int)
        val registry =
            buildRegistry {
                register<String>(Generator { "random-name" })
                register<Int>(Generator { 25 })
            }
        val user =
            registry.next<User> {
                override(User::name) with { "Alice" }
            }
        user.name shouldBe "Alice"
        user.age shouldBe 25
    }

    "property override takes priority over type override for the same field" {
        data class Container(val label: String)
        val registry =
            buildRegistry {
                register<String>(Generator { "default" })
            }
        val result =
            registry.next<Container> {
                override<String> { "type-level" }
                override(Container::label) with { "property-level" }
            }
        result.label shouldBe "property-level"
    }

    "type override propagates into auto-generated nested type" {
        data class Inner(val n: Int)

        data class Outer(val inner: Inner)
        val registry =
            buildRegistry {
                register<Int>(Generator { 1 })
            }
        val result =
            registry.next<Outer> {
                override<Int> { 999 }
            }
        result.inner.n shouldBe 999
    }

    // Issue 1: property override must be scoped to the declaring class
    // Bug: Inner::name (prop.name == "name") must NOT match Outer's "name" param
    "Inner::name override must not bleed into Outer::name when generating Outer" {
        data class Inner(val name: String)

        data class Outer(val inner: Inner, val name: String)
        val registry =
            buildRegistry {
                register<String>(Generator { "default" })
            }
        // We intentionally pass Inner::name as the override key — it must NOT affect Outer.name
        val result =
            registry.next<Outer> {
                override(Inner::name) with { "inner-override" }
            }
        // Outer.name was NOT overridden — it should be "default"
        result.name shouldBe "default"
    }

    // Issue 2: FixtureModule tests
    "fixtureModule generators are available after include" {
        val module =
            fixtureModule {
                register<String>(Generator { "from-module" })
            }
        val registry = buildRegistry { include(module) }
        registry.next<String>() shouldBe "from-module"
    }

    "multiple modules can be included" {
        val strings = fixtureModule { register<String>(Generator { "hello" }) }
        val ints = fixtureModule { register<Int>(Generator { 7 }) }
        val registry =
            buildRegistry {
                include(strings)
                include(ints)
            }
        registry.next<String>() shouldBe "hello"
        registry.next<Int>() shouldBe 7
    }

    "generator is called fresh on each next() call" {
        var counter = 0
        val registry = buildRegistry { register<Int>(Generator { counter++ }) }
        registry.next<Int>() shouldBe 0
        registry.next<Int>() shouldBe 1
    }

    "property override on non-data class with accessible primaryConstructor" {
        class Box(val value: String)
        val registry = buildRegistry { register<String>(Generator { "default" }) }
        val box =
            registry.next<Box> {
                override(Box::value) with { "overridden" }
            }
        box.value shouldBe "overridden"
    }

    "value class registered directly works as a field in auto-generated class" {
        data class User(val id: UserId, val name: String)
        val registry =
            buildRegistry {
                register<UserId>(Generator { UserId("u-123") })
                register<String>(Generator { "Alice" })
            }
        val user = registry.next<User>()
        user.id shouldBe UserId("u-123")
        user.name shouldBe "Alice"
    }

    "auto-generates data class with nullable field using non-nullable generator" {
        data class MaybeValue(val required: String, val optional: String?)
        val registry = buildRegistry { register<String>(Generator { "hello" }) }
        val result = registry.next<MaybeValue>()
        result.required shouldBe "hello"
        result.optional shouldBe "hello"
    }

    // Issue 2: RegistrationScope lazy resolution test
    "get<T>() in registration resolves type override at next() time not at registration time" {
        val module =
            fixtureModule {
                register<Int>(Generator { 10 })
                register<String> {
                    val n = get<Int>()
                    Generator { n.next().toString() }
                }
            }
        val registry = buildRegistry { include(module) }

        registry.next<String>() shouldBe "10"

        registry.next<String> {
            override<Int> { 99 }
        } shouldBe "99"
    }

    "get<T>() preserves override context through generator combinators" {
        val module =
            fixtureModule {
                register<Int>(Generator { 10 })
                register<String> {
                    get<Int>().map { it.toString() }
                }
            }
        val registry = buildRegistry { include(module) }

        registry.next<String>() shouldBe "10"

        registry.next<String> {
            override<Int> { 99 }
        } shouldBe "99"
    }
})
