# Kotest Integration

`kofixture-kotest` adds two things on top of `kofixture-core`:

- Kotest-friendly spec classes and test-scope helpers
- property-testing integration through `Arb`

## Spec Base Classes

Use one of the provided spec types, for example `KofixtureStringSpec`, `KofixtureFunSpec`, or `KofixtureFreeSpec`.

```kotlin
class CreateAccountRequestTest : KofixtureStringSpec() {
    override val fixtureModules = listOf(accountModule)

    init {
        "creates a valid request by default" {
            val request = next<CreateAccountRequest>()
            request.email shouldNotBe ""
        }
    }
}
```

Each spec gets its own registry instance. `kofixture-kotest` automatically layers its Kotest-backed primitive generators into that registry before your custom modules are applied.

## Arb Bridge

You can move between Kofixture generators and Kotest property generators:

```kotlin
val emails: Arb<String> = registry.arb()

val usernames = Generator { "user-${UUID.randomUUID()}" }.toArb()

val passwords = Arb.string(minSize = 12, maxSize = 24).toGenerator()
```

## Registering Arb Factories

When `kofixture-kotest` is on the classpath, both `buildRegistry {}` and `fixtureModule {}` accept `Arb`-returning factories:

```kotlin
val authModule =
    fixtureModule {
        register<String> { Arb.string(minSize = 12, maxSize = 24) }
    }
```

This works without adding Kotest types back into `kofixture-core`; the bridge is provided by the integration module.

## Manual Opt-In for Kotest Primitives

If you are not using `Kofixture*Spec`, but you still want Kotest-backed primitive generators, add the module explicitly:

```kotlin
val registry =
    buildRegistry {
        include(kotestPrimitivesModule)
    }
```

That is useful for ad-hoc registries built inside property tests.
