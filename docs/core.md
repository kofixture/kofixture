# Core Usage

`kofixture-core` is the standalone fixture engine. It does not depend on Kotest and can be used in plain JUnit, integration tests, CLI tests, or any custom harness.

## Building a Registry

```kotlin
val registry =
    buildRegistry {
        register<EmailAddress>(Generator { EmailAddress("john@example.com") })
        register<AccountId>(Generator { AccountId("acc-123") })
    }
```

Every registry starts with default primitive generators and then applies your registrations on top.

## Generating Objects

```kotlin
data class User(
    val id: AccountId,
    val email: EmailAddress,
    val age: Int,
)

val user = registry.next<User>()
```

Kofixture will:
- use your explicit generators first
- recurse through constructor parameters
- auto-generate collections
- reuse primitive defaults where possible

## Type Overrides

Type overrides affect the full generation graph for a single `next()` call:

```kotlin
val user = registry.next<User> {
    override<Int> { 42 }
}
```

That is useful when you want to force a specific value across nested types.

## Property Overrides

Property overrides target one field on one declaring type:

```kotlin
val user = registry.next<User> {
    override(User::email) with EmailAddress("custom@example.com")
}
```

You can register:
- a constant value: `with "invalid"`
- a factory: `with { randomValue() }`
- an existing generator: `with myGenerator`

## Collections

By default, generated collections use small random sizes. You can tune them per call:

```kotlin
val report = registry.next<Report> {
    collections {
        list = 3..3
        set = 2..4
        map = 1..1
    }
}
```

## Reusable Modules

Use `fixtureModule {}` when multiple tests need the same registrations:

```kotlin
val accountModule =
    fixtureModule {
        register<AccountId>(Generator { AccountId("acc-123") })
        register<EmailAddress>(Generator { EmailAddress("john@example.com") })
    }

val registry =
    buildRegistry {
        include(accountModule)
    }
```

Modules compose cleanly. Later registrations override earlier ones.
