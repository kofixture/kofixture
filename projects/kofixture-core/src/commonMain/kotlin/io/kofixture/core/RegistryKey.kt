package io.kofixture.core

import kotlin.reflect.KType

data class RegistryKey(
    val type: KType,
    val tag: String? = null,
)
