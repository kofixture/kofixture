package io.kofixture

import io.kotest.property.Arb
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

private val DEFAULT_COLLECTION_SIZE = 1..5

/** Configures the size ranges used when auto-generating collection types. */
data class CollectionSizeConfig(
    val list: IntRange = DEFAULT_COLLECTION_SIZE,
    val set: IntRange = DEFAULT_COLLECTION_SIZE,
    val map: IntRange = DEFAULT_COLLECTION_SIZE,
)

/** DSL receiver for [OverrideScope.collections]. */
class CollectionSizeScope {
    var list: IntRange = DEFAULT_COLLECTION_SIZE
    var set: IntRange = DEFAULT_COLLECTION_SIZE
    var map: IntRange = DEFAULT_COLLECTION_SIZE

    internal fun build(): CollectionSizeConfig = CollectionSizeConfig(list, set, map)
}

class Registry internal constructor(
    // Not @PublishedApi — no public inline fun accesses this field directly
    internal val generators: Map<KType, Generator<*>>,
) {
    /**
     * Generates a value of type [T], applying any overrides specified in [block].
     *
     * Type overrides propagate pre-construction through the entire generation graph.
     * Property overrides apply post-construction via `primaryConstructor.callBy()`.
     */
    inline fun <reified T> next(block: OverrideScope.() -> Unit = {}): T {
        val scope = OverrideScope().apply(block)
        val context =
            GenerationContext(
                typeOverrides = scope.typeOverrides,
                propertyOverrides = scope.propertyOverrides,
            )
        val raw = resolveAndGenerate<T>(typeOf<T>(), context, sizes = scope.collectionSizes)
        @Suppress("UNCHECKED_CAST")
        return applyPropertyOverrides(raw as Any, context) as T
    }

    /**
     * Resolves and generates a value for the given [type].
     *
     * Resolution order:
     * 1. Type override in the current [GenerationContext]
     * 2. Registered generator
     * 3. Collection auto-generation for [List], [Set], [Map]
     * 4. Non-nullable fallback (for nullable types — always produces a non-null value)
     * 5. Auto-generation via primary constructor reflection
     *
     * **Nullable parameters:** `String?` falls back to the `String` generator and always
     * produces a non-null value. Register `Generator<String?>` explicitly to produce `null`.
     *
     * @throws IllegalStateException if max generation depth is exceeded (recursive type detected)
     */
    fun <T> resolveAndGenerate(type: KType): T = resolveAndGenerate(type, GenerationContext.EMPTY, 0, CollectionSizeConfig())

    // @PublishedApi required: called from public inline fun next()
    @PublishedApi
    internal fun <T> resolveAndGenerate(
        type: KType,
        context: GenerationContext,
    ): T = resolveAndGenerate(type, context, 0, CollectionSizeConfig())

    @PublishedApi
    internal fun <T> resolveAndGenerate(
        type: KType,
        context: GenerationContext,
        sizes: CollectionSizeConfig,
    ): T = resolveAndGenerate(type, context, 0, sizes)

    @Suppress("CyclomaticComplexMethod", "ReturnCount", "UNCHECKED_CAST")
    private fun <T> resolveAndGenerate(
        type: KType,
        context: GenerationContext,
        depth: Int,
        sizes: CollectionSizeConfig,
    ): T {
        if (depth >= MAX_DEPTH) {
            error(
                "Max generation depth ($MAX_DEPTH) exceeded for type $type — " +
                    "likely a recursive type. Register a Generator<${type.classifier}> directly.",
            )
        }
        context.typeOverrides[type]?.let { return it.next(context) as T }
        generators[type]?.let { return it.next(context) as T }
        when (type.classifier) {
            List::class -> {
                val elementType = type.arguments[0].type ?: error("Unresolved type argument for List")
                return List(sizes.list.random()) { resolveAndGenerate<Any?>(elementType, context, depth + 1, sizes) } as T
            }

            Set::class -> {
                val elementType = type.arguments[0].type ?: error("Unresolved type argument for Set")
                return List(sizes.set.random()) { resolveAndGenerate<Any?>(elementType, context, depth + 1, sizes) }.toSet() as T
            }

            Map::class -> {
                val keyType = type.arguments[0].type ?: error("Unresolved key type argument for Map")
                val valueType = type.arguments[1].type ?: error("Unresolved value type argument for Map")
                return List(sizes.map.random()) {
                    resolveAndGenerate<Any?>(keyType, context, depth + 1, sizes) to
                        resolveAndGenerate<Any?>(valueType, context, depth + 1, sizes)
                }.toMap() as T
            }
        }
        if (type.isMarkedNullable) {
            val nonNullable =
                (type.classifier as? KClass<*>)
                    ?.createType(type.arguments, nullable = false)
            nonNullable?.let { generators[it]?.let { gen -> return gen.next(context) as T } }
        }
        val klass =
            type.classifier as? KClass<*>
                ?: error("Cannot generate value for $type")
        return generateViaReflection(klass, context, depth, sizes) as T
    }

    private fun collectConcreteSubclasses(klass: KClass<*>): List<KClass<*>> = klass.sealedSubclasses.flatMap { sub ->
        if (sub.isSealed) collectConcreteSubclasses(sub) else listOf(sub)
    }

    private fun generateViaReflection(
        klass: KClass<*>,
        context: GenerationContext,
        depth: Int,
        sizes: CollectionSizeConfig,
    ): Any {
        if (klass.isSealed) {
            val subclasses = collectConcreteSubclasses(klass)
            require(subclasses.isNotEmpty()) { "No eligible subclass for sealed ${klass.simpleName}" }
            return resolveAndGenerate<Any>(subclasses.random().createType(), context, depth + 1, sizes)
        }
        klass.objectInstance?.let { return it }
        val constructor =
            klass.primaryConstructor
                ?: error("No primary constructor for ${klass.simpleName}. Register a Generator<${klass.simpleName}> directly.")
        constructor.isAccessible = true
        val args = constructor.parameters.associateWith { resolveAndGenerate<Any?>(it.type, context, depth + 1, sizes) }
        return constructor.callBy(args)
    }

    // @PublishedApi required: called from public inline fun next()
    @Suppress("ReturnCount")
    @PublishedApi
    internal fun applyPropertyOverrides(
        obj: Any,
        context: GenerationContext,
    ): Any {
        if (context.propertyOverrides.isEmpty()) return obj
        val klass = obj::class
        val constructor = klass.primaryConstructor ?: return obj
        constructor.isAccessible = true
        val params =
            buildMap {
                for (param in constructor.parameters) {
                    val override =
                        context.propertyOverrides.entries.find { (prop, _) ->
                            prop.name == param.name &&
                                prop.parameters
                                    .firstOrNull()
                                    ?.type
                                    ?.classifier == klass
                        }
                    if (override != null) {
                        put(param, override.value.next(context))
                    } else {
                        val prop = klass.memberProperties.find { it.name == param.name }
                        if (prop != null) {
                            prop.getter.isAccessible = true
                            put(param, prop.getter.call(obj))
                        } else if (!param.isOptional) {
                            error(
                                "No backing property for required parameter '${param.name}' " +
                                    "in ${klass.simpleName}. Register a Generator<${klass.simpleName}> directly.",
                            )
                        }
                        // optional params with no backing property: omit → callBy uses default
                    }
                }
            }
        return constructor.callBy(params)
    }

    companion object {
        private const val MAX_DEPTH = 64
    }
}

class OverrideScope {
    @PublishedApi internal val typeOverrides = mutableMapOf<KType, Generator<*>>()

    @PublishedApi internal val propertyOverrides = mutableMapOf<KProperty1<*, *>, Generator<*>>()

    @PublishedApi internal var collectionSizes = CollectionSizeConfig()

    /** Overrides the type [T] with [gen] for this generation call only. */
    inline fun <reified T> override(gen: Generator<T>) {
        typeOverrides[typeOf<T>()] = gen
    }

    /** Overrides the type [T] with a value factory for this generation call only. */
    inline fun <reified T> override(noinline factory: () -> T) {
        override(Generator(factory))
    }

    /**
     * Begins a property override. Chain with [PropertyBound.with] to provide the value factory:
     * ```
     * override(User::name) with { "Alice" }
     * ```
     * The two-step design pins `T` from the property reference before the factory lambda is
     * evaluated, ensuring compile-time type safety (e.g. passing `{ 42 }` for a `String`
     * property is a type error).
     */
    fun <S : Any, T> override(prop: KProperty1<S, T>): PropertyBound<S, T> = PropertyBound(prop, this)

    /** Configures the size ranges used when auto-generating [List], [Set], and [Map] values. */
    fun collections(block: CollectionSizeScope.() -> Unit) {
        collectionSizes = CollectionSizeScope().apply(block).build()
    }
}

/** Intermediate holder returned by [OverrideScope.override]; complete with [with]. */
class PropertyBound<S : Any, T> internal constructor(
    private val prop: KProperty1<S, T>,
    private val scope: OverrideScope,
) {
    /** Registers a constant [value] for this property. */
    infix fun with(value: T) {
        scope.propertyOverrides[prop] = Generator { value }
    }

    /**
     * Registers [factory] as the value supplier for this property.
     * Called on every [Registry.next] invocation, so randomised factories work as expected:
     * ```
     * override(User::name).with { Random.nextInt().toString() }
     * ```
     * Note: prefer dot-call syntax over infix for accurate IDE parameter hints.
     */
    infix fun with(factory: () -> T) {
        scope.propertyOverrides[prop] = Generator { factory() }
    }

    /** Registers an existing [gen] as the generator for this property. */
    infix fun with(gen: Generator<T>) {
        scope.propertyOverrides[prop] = gen
    }
}

class RegistryBuilder {
    @PublishedApi internal val generators = mutableMapOf<KType, Generator<*>>()

    @PublishedApi internal val registryRef: Lazy<Registry> = lazy { Registry(generators.toMap()) }

    inline fun <reified T> register(generator: Generator<T>) {
        generators[typeOf<T>()] = generator
    }

    inline fun <reified T> register(noinline factory: RegistrationScope.() -> Generator<T>) {
        generators[typeOf<T>()] =
            Generator.contextual { context ->
                factory(RegistrationScope.bound(registryRef, context)).next(context)
            }
    }

    @JvmName("registerArbFactory")
    inline fun <reified T> register(noinline factory: RegistrationScope.() -> Arb<T>) {
        generators[typeOf<T>()] =
            Generator.contextual { context ->
                factory(RegistrationScope.bound(registryRef, context)).toGenerator().next(context)
            }
    }

    fun registerByType(
        type: KType,
        generator: Generator<*>,
    ) {
        generators[type] = generator
    }

    fun include(module: FixtureModule) {
        module.applyTo(this)
    }

    internal fun build(): Registry = registryRef.value
}

/**
 * Creates a [Registry] with default primitive generators pre-registered.
 * Registrations provided in [block] override the defaults.
 *
 * **Limitation:** Collection types (`List<T>`, `Set<T>`, `Map<K,V>`) are not auto-generated.
 * Register an explicit `Generator<List<T>>` (or similar) for any collection fields you need.
 *
 * @see RegistryBuilder
 */
fun buildRegistry(block: RegistryBuilder.() -> Unit): Registry {
    val builder = RegistryBuilder()
    defaultGenerators.forEach { (type, gen) -> builder.registerByType(type, gen) }
    block(builder)
    return builder.build()
}

/** Returns a [Generator] that produces values of type [T] using this registry. */
inline fun <reified T> Registry.generator(noinline block: OverrideScope.() -> Unit = {}): Generator<T> = Generator { next<T>(block) }

/** Returns an [Arb] that produces values of type [T] using this registry. */
inline fun <reified T> Registry.arb(noinline block: OverrideScope.() -> Unit = {}): Arb<T> = generator<T>(block).toArb()
