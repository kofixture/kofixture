package co.kofixtures.core

/**
 * An immutable, composable container of [Generator] registrations.
 *
 * A module captures a builder block and replays it when [FixtureRegistryBuilder.includes] is
 * called, allowing sets of generators to be defined once and shared across multiple registries
 * or test specs. Modules are additive: including the same module twice registers its generators
 * twice, with the last registration winning for a given type.
 *
 * Create instances via the [fixtureModule] DSL function.
 */
class FixtureModule internal constructor(
    internal val block: FixtureRegistryBuilder.() -> Unit,
)

/**
 * DSL entry point that creates a [FixtureModule] from a builder block.
 *
 * The [block] receives a [FixtureRegistryBuilder] and should call [FixtureRegistryBuilder.register]
 * to declare generators. The block is stored and replayed whenever this module is included in a
 * registry — it is not executed eagerly.
 */
fun fixtureModule(block: FixtureRegistryBuilder.() -> Unit): FixtureModule = FixtureModule(block)
