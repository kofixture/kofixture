# Contributing to Kofixture

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you agree to uphold this code.

## Prerequisites

- JDK 21 (`java -version` should show 21+)
- Git

## Building the Library

```bash
cd projects
./gradlew build
```

## Running Tests

```bash
cd projects
./gradlew jvmTest
```

## Building Examples

The examples module uses a [composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
so no local publishing step is required — Gradle substitutes Maven coordinates with the local source projects automatically.

```bash
cd examples
./gradlew build
```

## Code Conventions

- **Formatting**: Kotlin DSL everywhere. Run `./gradlew spotlessApply` before committing.
- **Static analysis**: `./gradlew detekt` must pass.
- **API changes**: Run `./gradlew apiDump` and commit the updated `api/` files.
- **Branching**: `feature/<name>`, `fix/<name>`, `chore/<name>`
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/)
  - `feat(core): add nullable derivation`
  - `fix(ksp): correct sealed class ordering`
  - `chore(build): bump Kotlin to 2.3.10`

## Pull Requests

Before opening a PR:
- [ ] Tests added or updated
- [ ] `./gradlew spotlessCheck` passes
- [ ] `./gradlew detekt` passes
- [ ] API changes: ran `./gradlew apiDump` and committed updated `api/` files
- [ ] `CHANGELOG.md` updated if user-visible change
