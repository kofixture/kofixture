package co.kofixtures.core

internal expect fun <K : Any, V : Any> concurrentMutableMapOf(): MutableMap<K, V>
