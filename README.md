# Kofixture

> ⚠️ **Pre-release** — not yet published to Maven Central.

Kofixture is a Kotlin Multiplatform library for generating type-safe test fixtures.
It removes boilerplate from test setup by providing a composable, declarative API
for producing arbitrarily complex object graphs from a registry of generators.

## Modules

| Artifact | Description |
|---|---|
| `kofixture-core` | Core fixture generation engine (KMP: JVM, JS, Native) |
| `kofixture-kotest-arb` | Kotest `Arb<T>` integration (KMP) |
| `kofixture-ksp` | KSP processor for automatic fixture registration (JVM only) |

## Quickstart

```kotlin
// 1. Build a registry
val registry = buildRegistry {
    register<User> { User(next<String>(), next<Int>()) }
}

// 2. Sample values
val user: User = registry.sample()

// 3. Override per-sample
val named: User = registry.sample { override(User::name) { "Alice" } }
```

## Dependency Coordinates

```kotlin
// build.gradle.kts
testImplementation("io.github.kofixture:kofixture-core:0.1.0-SNAPSHOT")
testImplementation("io.github.kofixture:kofixture-kotest-arb:0.1.0-SNAPSHOT")
kspTest("io.github.kofixture:kofixture-ksp:0.1.0-SNAPSHOT")
```

Repositories: `mavenLocal()` (until published to Maven Central)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). License: [Apache-2.0](LICENSE).

## Documentation

- [docs/quickstart/](docs/quickstart/)
- [docs/reference/](docs/reference/)
- [docs/setup/](docs/setup/)
