package io.kofixture.core

internal expect fun <K : Any, V : Any> concurrentMutableMapOf(): MutableMap<K, V>
