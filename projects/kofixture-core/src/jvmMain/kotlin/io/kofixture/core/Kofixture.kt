package io.kofixture.core

import kotlin.reflect.KClass

/**
 * Marks an `object` declaration as the source of a KSP-generated [FixtureModule].
 *
 * The processor scans [packages] and explicit [classes] to collect eligible types — `data class`,
 * `enum class`, `sealed class`, `sealed interface`, and `object` — and emits a `FixtureModule`
 * property plus typed [OverrideScope] extension members for each type found.
 *
 * Example:
 * ```kotlin
 * @Kofixture(packages = ["com.example.domain"])
 * object Fixtures
 * // Generates: val fixtures: FixtureModule = ...
 * ```
 *
 * @see FixtureModule
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Kofixture(
    /**
     * Packages to scan for eligible types. All `data class`, `enum class`, `sealed class`,
     * `sealed interface`, and `object` types found in these packages (and their sub-packages)
     * are included in the generated module.
     */
    val packages: Array<String> = [],
    /**
     * Explicit list of fully-qualified class names to include in addition to [packages] scanning.
     * Use this when a type lives in a package that should not be scanned wholesale.
     */
    val classes: Array<KClass<*>> = [],
    /**
     * Name of the generated `FixtureModule` property. Defaults to the annotated object's simple
     * name converted to lowercase (e.g., `object Fixtures` → `val fixtures`).
     */
    val moduleName: String = "",
)
