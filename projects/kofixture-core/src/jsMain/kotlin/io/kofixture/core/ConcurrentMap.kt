package io.kofixture.core

internal actual fun <K : Any, V : Any> concurrentMutableMapOf(): MutableMap<K, V> = mutableMapOf()
