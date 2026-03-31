package io.kofixture.core

import io.kofixture.FixtureModule
import io.kofixture.Registry
import io.kofixture.buildRegistry

/**
 * Global context managing per-spec [Registry] instances.
 * Registries are created in `beforeSpec` and released in `afterSpec` via [KofixtureExtension].
 */
object KofixtureContext {
    /** Default modules included when a spec doesn't define its own [KofixtureTest.fixtureModules]. */
    var defaultModules: List<FixtureModule> = emptyList()

    private val registries: MutableMap<KofixtureTest, Registry> = mutableMapOf()

    fun registryFor(spec: KofixtureTest): Registry = registries[spec]
        ?: error(
            "No fixture registry found for ${spec::class.simpleName}. " +
                "Make sure KofixtureExtension is registered in AbstractProjectConfig.",
        )

    fun buildFor(spec: KofixtureTest) {
        val modules = spec.fixtureModules.ifEmpty { defaultModules }
        require(modules.isNotEmpty()) {
            "No fixture modules configured for ${spec::class.simpleName}. " +
                "Either set KofixtureContext.defaultModules in ProjectConfig, " +
                "or override fixtureModules in the spec."
        }
        registries[spec] = buildRegistry { modules.forEach { include(it) } }
    }

    fun releaseFor(spec: KofixtureTest) {
        registries.remove(spec)
    }
}
