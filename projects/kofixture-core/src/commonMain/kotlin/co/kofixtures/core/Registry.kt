package co.kofixtures.core

import kotlin.experimental.ExperimentalTypeInference
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.typeOf

class FixtureRegistry internal constructor(
    internal val factories: Map<RegistryKey, FactoryScope.() -> Generator<*>>,
    val collectionConfig: CollectionConfig = CollectionConfig(),
) {
    // Pre-built index for nullable derivation: (classifier, args, tag) → non-null KType
    private val nullableIndex: Map<Triple<KClassifier?, List<KTypeProjection>, String?>, KType> =
        factories.keys
            .filter { !it.type.isMarkedNullable }
            .associate { key ->
                Triple(key.type.classifier, key.type.arguments, key.tag) to key.type
            }

    inline fun <reified T> generator(
        tag: String? = null,
        noinline block: OverrideScope<T>.() -> Unit = {},
    ): Generator<T> {
        val scope = OverrideScope<T>(this).apply(block)
        val active = ActiveOverrides.from(scope)
        return resolve(typeOf<T>(), tag, active)
    }

    inline fun <reified T> sample(
        random: Random = Random.Default,
        tag: String? = null,
        noinline block: OverrideScope<T>.() -> Unit = {},
    ): T {
        val scope = OverrideScope<T>(this).apply(block)
        val active = ActiveOverrides.from(scope)
        return resolve<T>(typeOf<T>(), tag, active).next(random)
    }

    /**
     * Recursive type resolver.
     *
     * Resolution order:
     *   1. Type-based override   — active.byType[type]
     *   2. Registry with tag     — factories[type, tag]
     *   3. Registry primary      — factories[type, null]
     *   4. Nullable derivation   — T? → resolve(T) + 50/50 null
     *   5. Collection derivation — List/Set/Map
     *   6. Error
     *
     * Named overrides are checked in [FactoryScope.get], not here.
     */
    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T> resolve(
        type: KType,
        tag: String? = null,
        active: ActiveOverrides,
    ): Generator<T> {
        val scope = FactoryScope(this, active)
        val activeOverrideResult = { active.resolveType(type)?.let { it as Generator<T> } }
        val tagResult = { tag?.let { factories[RegistryKey(type, it)]?.let { it(scope) as Generator<T> } } }
        val nonTagResult = { factories[RegistryKey(type, null)]?.let { it(scope) as Generator<T> } }
        val nullableResult = {
            if (type.isMarkedNullable) {
                val lookupKey = Triple(type.classifier, type.arguments, tag)
                val fallbackKey = Triple(type.classifier, type.arguments, null)
                (nullableIndex[lookupKey] ?: nullableIndex[fallbackKey])?.let { nonNullType ->
                    val inner = resolve<T>(nonNullType, tag, active)
                    Generator { random ->
                        if (random.nextBoolean()) inner.next(random) else null as T
                    }
                }
            } else {
                null
            }
        }
        val collectionResult = { deriveCollection<T>(type, active) }

        return listOf(activeOverrideResult, tagResult, nonTagResult, nullableResult, collectionResult)
            .map { it() }
            .firstOrNull { it != null } ?: run {
            val tagInfo = if (tag != null) " (tag=\"$tag\")" else ""
            error(
                "No generator registered for $type$tagInfo.\n" +
                    "Registered keys: ${factories.keys.joinToString()}",
            )
        }
    }

    @Suppress("UNCHECKED_CAST", "ReturnCount")
    private fun <T> deriveCollection(
        type: KType,
        active: ActiveOverrides,
    ): Generator<T>? {
        val classifier = type.classifier as? KClass<*> ?: return null
        val args = type.arguments
        val effectiveConfig = active.collectionConfig ?: collectionConfig

        return when (classifier) {
            List::class, Collection::class, Iterable::class -> {
                val elementType = args.firstOrNull()?.type ?: return null
                val elementGen = resolve<Any?>(elementType, active = active)
                val range = effectiveConfig.listSize
                Generator { random ->
                    List(random.nextInt(range.first, range.last + 1)) { elementGen.next(random) } as T
                }
            }

            Set::class -> {
                val elementType = args.firstOrNull()?.type ?: return null
                val elementGen = resolve<Any?>(elementType, active = active)
                val range = effectiveConfig.setSize
                Generator { random ->
                    buildSet {
                        repeat(random.nextInt(range.first, range.last + 1)) { add(elementGen.next(random)) }
                    } as T
                }
            }

            Map::class -> {
                val keyType = args.getOrNull(0)?.type ?: return null
                val valueType = args.getOrNull(1)?.type ?: return null
                val keyGen = resolve<Any?>(keyType, active = active)
                val valueGen = resolve<Any?>(valueType, active = active)
                val range = effectiveConfig.mapSize
                Generator { random ->
                    buildMap {
                        repeat(random.nextInt(range.first, range.last + 1)) {
                            put(keyGen.next(random), valueGen.next(random))
                        }
                    } as T
                }
            }

            else -> {
                null
            }
        }
    }
}

class FixtureRegistryBuilder {
    @PublishedApi
    internal val factories = mutableMapOf<RegistryKey, FactoryScope.() -> Generator<*>>()

    var collectionConfig: CollectionConfig = CollectionConfig()

    fun collections(block: CollectionConfigBuilder.() -> Unit) {
        collectionConfig = CollectionConfigBuilder().apply(block).build()
    }

    fun includes(vararg modules: FixtureModule) {
        modules.forEach { it.block(this) }
    }

    fun <T> register(
        type: KType,
        tag: String? = null,
        factory: FactoryScope.() -> Generator<T>,
    ) {
        @Suppress("UNCHECKED_CAST")
        factories[RegistryKey(type, tag)] = factory as FactoryScope.() -> Generator<*>
    }

    fun build(): FixtureRegistry = FixtureRegistry(factories.toMap(), collectionConfig)
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T> FixtureRegistryBuilder.register(
    tag: String? = null,
    noinline factory: FactoryScope.() -> Generator<T>,
) = register(typeOf<T>(), tag, factory)

fun buildRegistry(block: FixtureRegistryBuilder.() -> Unit): FixtureRegistry =
    FixtureRegistryBuilder().apply(block).build()

/**
 * Public helper for KSP-generated code.
 * Resolves a [Generator] for [type] respecting the provided [activeOverrides].
 */
fun <T> FixtureRegistry.generatorFor(
    type: KType,
    tag: String?,
    activeOverrides: ActiveOverrides,
): Generator<T> = resolve(type, tag, activeOverrides)
