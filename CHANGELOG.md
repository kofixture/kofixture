# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-03-10

### Added

- **`kofixture-core`** — Core fixture generation engine (KMP: JVM, JS, Native)
  - `Generator<T>` fun interface: `next(Random): T`
  - `FixtureModule` and `fixtureModule { }` DSL for composable generator registries
  - `FixtureRegistry` with typed `sample<T>()` and `sample<T> { override }` APIs
  - `OverrideScope<T>` for type-safe per-sample value overrides, including nested types
  - `KofixtureTest` interface with `sample<T>`, `generator<T>` property delegates
  - `KofixtureContext` for per-spec registry lifecycle management
  - Collection and nullable type derivation (auto-generates `List<T>`, `T?` from registered `T`)
  - JVM: reflection-based `registerOf<T>()` for data classes via `kotlin-reflect`

- **`kofixture-kotest-arb`** — Kotest `Arb<T>` integration (KMP: JVM, JS, Native)
  - `ArbGenerator<T>` wrapping `Arb<T>` as a `Generator<T>`
  - `register<T>(Arb<T>)` and `override(prop, Arb<T>)` overloads
  - `arb<T>()` property delegate alongside `sample<T>()` and `generator<T>()`

- **`kofixture-ksp`** — KSP annotation processor (JVM only)
  - `@Kofixture(packages, classes, moduleName)` annotation targeting `object` declarations
  - Generates a typed `FixtureModule` (`val xxxFixtures`) for all discovered classes
  - Supports data classes, enums, sealed classes/interfaces, and objects
  - Generates `OverrideScope<T>` extension properties and functions per class,
    including nested-type scope lambdas for deep object graph overrides

- **`kofixture-kotest`** — Kotest spec base classes (KMP: JVM, JS, Native)
  - `KofixtureFunSpec`, `KofixtureBehaviorSpec`, `KofixtureDescribeSpec`,
    `KofixtureExpectSpec`, `KofixtureFeatureSpec`, `KofixtureFreeSpec`,
    `KofixtureShouldSpec`, `KofixtureStringSpec`, `KofixtureWordSpec` —
    base classes for all lambda-accepting Kotest spec styles; delegates are in scope
    inside the constructor lambda
  - `KofixtureAnnotationSpec` (JVM only) — base class for `AnnotationSpec`
  - `KofixtureListener` — `BeforeSpecListener`/`AfterSpecListener` for managing
    registry lifecycle on any spec implementing `KofixtureTest`
