package co.kofixtures.core

/**
 * Global context managing per-spec [FixtureRegistry] instances.
 * Registries are created in beforeSpec and released in afterSpec.
 */
object KofixtureContext {
    /** Default modules used when a spec does not define its own. */
    var defaultModules: List<FixtureModule> = emptyList()

    @PublishedApi
    internal val registries: MutableMap<KofixtureTest, FixtureRegistry> = concurrentMutableMapOf()

    fun registryFor(spec: KofixtureTest): FixtureRegistry = registries[spec] ?: error(
        "No fixture registry found for ${spec::class.simpleName}. " +
            "Make sure buildRegistry() is called in beforeSpec, or call buildRegistry() manually.",
    )

    internal fun buildFor(spec: KofixtureTest) {
        val modules = spec.fixtureModules.ifEmpty { defaultModules }
        require(modules.isNotEmpty()) {
            "No fixture modules configured for ${spec::class.simpleName}. " +
                "Either set KofixtureContext.defaultModules in ProjectConfig, " +
                "or override fixtureModules in the spec."
        }
        registries[spec] = buildRegistry { modules.forEach { includes(it) } }
    }

    fun releaseFor(spec: KofixtureTest) {
        registries.remove(spec)
    }
}
