package co.kofixtures.core

import kotlin.reflect.KFunction
import kotlin.reflect.typeOf

/**
 * Registers a generator for [T] that constructs instances via reflection using [constructor].
 * Named overrides are applied per-parameter by name lookup.
 *
 * JVM only — requires `kotlin-reflect`.
 */
inline fun <reified T> FixtureRegistryBuilder.registerOf(
    constructor: KFunction<T>,
    tag: String? = null,
) = register(typeOf<T>(), tag) {
    Generator { random ->
        val args =
            constructor.parameters.map { param ->
                val paramName =
                    param.name
                        ?: error(
                            "Constructor parameter at index ${param.index} in '${constructor.name}' has no name. " +
                                "Ensure the class is compiled with parameter names (default in Kotlin).",
                        )
                val namedKey = NamedOverrideKey(typeOf<T>(), paramName)

                @Suppress("UNCHECKED_CAST")
                val gen: Generator<*> =
                    activeOverrides.resolveNamed(namedKey)
                        ?: registry.resolve<Any?>(param.type, null, activeOverrides)
                gen.next(random)
            }
        @Suppress("SpreadOperator")
        constructor.call(*args.toTypedArray())
    }
}
