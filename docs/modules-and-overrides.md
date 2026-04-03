# Fixture Modules And Overrides

This page covers the two main extension points in Kofixture: reusable modules and per-call overrides.

## Fixture Modules

Modules package registrations into a reusable unit:

```kotlin
val billingModule =
    fixtureModule {
        register<Currency>(Generator { Currency.getInstance("USD") })
        register<Money> {
            Generator {
                Money(
                    amount = BigDecimal("9.99"),
                    currency = get<Currency>().next(),
                )
            }
        }
    }
```

Why modules matter:
- keep test setup close to the domain
- avoid duplicating value-object generators
- let integration tests share the same defaults as unit tests

## Module Ordering

Modules are applied in order. Later registrations win:

```kotlin
val registry =
    buildRegistry {
        include(commonModule)
        include(testSpecificModule)
    }
```

That makes it easy to define a broad baseline and then layer scenario-specific overrides on top.

## `get<T>()` Inside Registrations

Inside registration factories you can ask for other generators lazily:

```kotlin
register<CreateAccountRequest> {
    val email = get<EmailAddress>()
    val password = get<RawPassword>()

    Generator {
        CreateAccountRequest(
            email = email.next().value,
            password = password.next().value,
        )
    }
}
```

`get<T>()` respects the current override context, so scenario-specific overrides still propagate correctly.

## Per-Call Overrides

Overrides are intentionally scoped to one generation call:

```kotlin
val invalidRequest = registry.next<CreateAccountRequest> {
    override(CreateAccountRequest::email) with "invalid"
}
```

That keeps fixtures predictable:
- modules define the baseline
- overrides define the scenario

## Choosing Between Module And Override

Use a module when:
- the registration belongs to a domain concept
- multiple tests need the same setup
- you want to share a baseline across test types

Use an override when:
- the value only matters in one scenario
- you are expressing an edge case
- the rest of the object graph should stay generated
