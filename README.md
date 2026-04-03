# Kofixture

Kofixture is a Kotlin fixture library for tests. It gives you a small registry-based DSL for generating realistic objects, overriding only the fields that matter, and composing reusable fixture modules without reflection-heavy setup code in every test.

## Modules

| Artifact | Purpose |
| --- | --- |
| `io.github.kofixture:kofixture-core` | Core fixture engine, generators, registry DSL, primitive defaults |
| `io.github.kofixture:kofixture-kotest` | Kotest spec helpers, `Arb` bridge, Kotest-backed primitive generators |

## Installation

```kotlin
dependencies {
    testImplementation("io.github.kofixture:kofixture-core:0.2.2")
}
```

Add Kotest integration only if you want Kotest spec helpers or `Arb` support:

```kotlin
dependencies {
    testImplementation("io.github.kofixture:kofixture-kotest:0.2.2")

    testImplementation("io.kotest:kotest-runner-junit5:6.1.6")
}
```

## Quickstart

### Core

Use `buildRegistry {}` when you want fixtures without pulling in Kotest:

```kotlin
import io.kofixture.Generator
import io.kofixture.buildRegistry

data class EmailAddress(val value: String)
data class User(val email: EmailAddress, val age: Int)

val registry =
    buildRegistry {
        register<EmailAddress>(Generator { EmailAddress("john@example.com") })
    }

val user = registry.next<User>()
val adult = registry.next<User> {
    override(User::age) with 42
}
```

What you get out of the box:
- Primitive generators for `String`, numbers, `Boolean`, `UUID`, `Locale`, `ZoneId`, and `Instant`
- Reflection-based generation for data classes and constructor-backed types
- Collection generation for `List`, `Set`, and `Map`
- Scoped type overrides and property overrides
- Reusable `fixtureModule {}` blocks

### Kotest

With `kofixture-kotest`, you can use the same registry API plus Kotest spec helpers and `Arb` integration:

```kotlin
import io.kofixture.Generator
import io.kofixture.KofixtureStringSpec
import io.kotest.property.Arb
import io.kotest.matchers.shouldBe

data class SignupRequest(val email: String, val password: String)

private val authFixtures =
    fixtureModule {
        register<String> { Arb.string(minSize = 12, maxSize = 24) }
        register<SignupRequest> {
            Generator {
                SignupRequest(
                    email = "tester@example.com",
                    password = "correct-horse-battery-staple",
                )
            }
        }
    }

class SignupRequestTest : KofixtureStringSpec() {
    override val fixtureModules = listOf(authFixtures)

    init {
        "supports property overrides" {
            val request = next<SignupRequest> {
                override(SignupRequest::email) with "invalid"
            }

            request.email shouldBe "invalid"
        }
    }
}
```

`kofixture-kotest` also adds:
- `Registry.arb<T>()`
- `Generator<T>.toArb()`
- `Arb<T>.toGenerator()`
- `register { Arb<T> }` in `buildRegistry {}` and `fixtureModule {}`
- Kotest-backed primitive overrides for `Kofixture*Spec` users

## Examples

The repository ships with runnable examples in [`examples/`](./examples):

- `examples/core-only` shows manual registry usage with `kofixture-core`
- `examples/kotest` shows `KofixtureStringSpec`, fixture modules, and `Arb` registration

Both examples use a composite build, so they exercise the local sources directly instead of relying on a published version.

## Documentation

- [Core usage](./docs/core.md)
- [Kotest integration](./docs/kotest.md)
- [Fixture modules and overrides](./docs/modules-and-overrides.md)

## Current Direction

Kofixture v2 is intentionally smaller than the old KSP-based design:

- no generated fixture registries
- no annotation processor
- one explicit registry model
- optional Kotest integration layered on top of a clean core

That keeps the library easier to understand, easier to debug, and easier to extend.
