package co.kofixtures.core

/** Composable unit of fixture configuration. Created via [fixtureModule]. */
class FixtureModule internal constructor(
    internal val block: FixtureRegistryBuilder.() -> Unit,
)

fun fixtureModule(block: FixtureRegistryBuilder.() -> Unit): FixtureModule = FixtureModule(block)
