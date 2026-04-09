package io.kofixture.core

import io.kofixture.FixtureModule
import io.kofixture.Registry
import io.kofixture.buildRegistry
import io.kofixture.kotestPrimitivesModule

/**
 * Global context managing per-spec [Registry] instances.
 * Registries are created in `beforeSpec` and released in `afterSpec` via [KofixtureExtension].
 */
object KofixtureContext {
    /** Default modules included when a spec doesn't define its own [KofixtureTest.fixtureModules]. */
    var defaultModules: List<FixtureModule> = emptyList()

    private val registries: MutableMap<KofixtureTest, Registry> = mutableMapOf()

    fun registryFor(spec: KofixtureTest): Registry = registries.getOrPut(spec) { createRegistry(spec) }

    fun buildFor(spec: KofixtureTest) {
        registries[spec] = createRegistry(spec)
    }

    fun releaseFor(spec: KofixtureTest) {
        registries.remove(spec)
    }

    private fun createRegistry(spec: KofixtureTest): Registry {
        val modules = spec.fixtureModules.ifEmpty { defaultModules }
        require(modules.isNotEmpty()) {
            "No fixture modules configured for ${spec::class.simpleName}. " +
                "Either set KofixtureContext.defaultModules in ProjectConfig, " +
                "or override fixtureModules in the spec."
        }
        return buildRegistry {
            include(kotestPrimitivesModule)
            modules.forEach { include(it) }
        }
    }
}
