package co.kofixtures.core

/** Default size ranges used when auto-deriving collection types. */
data class CollectionConfig(
    val listSize: IntRange = 1..5,
    val setSize: IntRange = 1..5,
    val mapSize: IntRange = 1..5,
)

/** DSL builder for [CollectionConfig]. Used inside [buildRegistry] or [OverrideScope]. */
class CollectionConfigBuilder {
    var listSize: IntRange = 1..5
    var setSize: IntRange = 1..5
    var mapSize: IntRange = 1..5

    internal fun build() = CollectionConfig(listSize, setSize, mapSize)
}
