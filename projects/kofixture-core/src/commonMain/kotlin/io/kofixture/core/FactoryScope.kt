package io.kofixture.core

import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

/**
 * Scope available inside a factory lambda during resolve.
 * Provides access to other generators via [get] and [sample], respecting active overrides.
 *
 * Usage:
 * ```kotlin
 * buildRegistry {
 *     register<Person> {
 *         gen { Person(sample(Person::name, it), sample(Person::age, it)) }
 *     }
 * }
 * ```
 */
class FactoryScope(
    val registry: FixtureRegistry,
    val activeOverrides: ActiveOverrides,
) {
    inline fun <reified Owner : Any, reified Prop> get(
        property: KProperty1<Owner, Prop>,
        tag: String? = null,
    ): Generator<Prop> {
        val namedKey = NamedOverrideKey(typeOf<Owner>(), property.name)
        @Suppress("UNCHECKED_CAST")
        return activeOverrides.resolveNamed(namedKey) as? Generator<Prop>
            ?: registry.resolve(typeOf<Prop>(), tag, activeOverrides)
    }

    inline fun <reified Owner : Any, reified Prop> sample(
        property: KProperty1<Owner, Prop>,
        random: Random = Random,
        tag: String? = null,
    ): Prop = get(property, tag).next(random)
}
