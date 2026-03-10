# Kofixture

Kofixture is a Kotlin Multiplatform library for generating type-safe test fixtures.
It removes boilerplate from test setup with a composable, declarative API for producing
arbitrarily complex object graphs — including nested types, enums, sealed hierarchies, and nullables.

## Modules

| Artifact | Targets | Description |
|---|---|---|
| `kofixture-core` | JVM, JS, Native | Core fixture generation engine |
| `kofixture-kotest-arb` | JVM, JS, Native | Kotest `Arb<T>` integration |
| `kofixture-kotest` | JVM, JS, Native | Kotest spec base classes and lifecycle listener |
| `kofixture-ksp` | JVM | KSP processor — auto-generates fixture registrations |

## Quickstart

### With KSP + Kotest (recommended)

**1. Annotate a companion object to generate a fixture module:**

```kotlin
// src/test/kotlin/com/example/Fixtures.kt
@Kofixture(packages = ["com.example"])
object Fixtures
```

KSP generates a `fixtures` property containing all generators for `User`, `Address`, etc.

**2. Write tests using a spec base class:**

```kotlin
class UserTest : KofixtureFunSpec({
    val user by sample<User> { name = "Alice" }
    val profile by sample<Profile> {
        user { name = "Bob" }
        address { city = "Warsaw" }
    }

    test("overrides name") { user.name shouldBe "Alice" }
    test("overrides nested type") { profile.user.name shouldBe "Bob" }
}) {
    override val fixtureModules = listOf(fixtures)
}
```

Delegates (`sample<T>`, `generator<T>`, `arb<T>`) are resolved fresh per test via property delegation.

### Without KSP (manual registry)

```kotlin
val registry = buildRegistry {
    register<String> { Generator { random -> "str-${random.nextInt(100_000)}" } }
    register<User> { User(next<String>(), next<Int>()) }
}

val user: User = registry.sample()
val named: User = registry.sample { name = "Alice" }
```

## Installation

```kotlin
// build.gradle.kts
testImplementation("io.github.kofixture:kofixture-core:0.1.0")

// + Kotest integration (pick one or both)
testImplementation("io.github.kofixture:kofixture-kotest-arb:0.1.0")
testImplementation("io.github.kofixture:kofixture-kotest:0.1.0")

// + KSP processor (auto-generates fixture modules from annotated objects)
kspTest("io.github.kofixture:kofixture-ksp:0.1.0")
testCompileOnly("io.github.kofixture:kofixture-ksp:0.1.0") // annotation visibility
```

```kotlin
// settings.gradle.kts
pluginManagement {
    plugins {
        id("com.google.devtools.ksp") version "2.3.6"
    }
}
```

Artifacts are published to Maven Central — no extra repository configuration needed.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). License: [Apache-2.0](LICENSE).
