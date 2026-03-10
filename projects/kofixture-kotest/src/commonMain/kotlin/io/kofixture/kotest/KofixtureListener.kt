package io.kofixture.kotest

import io.kofixture.core.KofixtureContext
import io.kofixture.core.KofixtureTest
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec

/**
 * Kotest listener that manages the [KofixtureTest] registry lifecycle.
 *
 * Install it in your Kotest project configuration:
 * ```kotlin
 * class ProjectConfig : AbstractProjectConfig() {
 *     override val listeners = listOf(KofixtureListener())
 * }
 * ```
 *
 * Alternatively, use the spec base classes ([KofixtureFunSpec], [KofixtureDescribeSpec], etc.)
 * which manage their own lifecycle and do not require this listener.
 */
class KofixtureListener : BeforeSpecListener, AfterSpecListener {
    override suspend fun beforeSpec(spec: Spec) {
        if (spec is KofixtureTest) spec.buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        if (spec is KofixtureTest) KofixtureContext.releaseFor(spec)
    }
}
