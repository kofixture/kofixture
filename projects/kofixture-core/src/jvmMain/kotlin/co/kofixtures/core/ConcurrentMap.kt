package co.kofixtures.core

import java.util.concurrent.ConcurrentHashMap

internal actual fun <K : Any, V : Any> concurrentMutableMapOf(): MutableMap<K, V> = ConcurrentHashMap()
