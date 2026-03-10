package co.kofixtures.kotest

import co.kofixtures.core.KofixtureContext
import co.kofixtures.core.KofixtureTest
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec

// AnnotationSpec is JVM-only in Kotest; this wrapper is therefore also JVM-only.

/**
 * [AnnotationSpec] base class that integrates [KofixtureTest]. Unlike the lambda-based spec
 * classes, there is no constructor body parameter — define test methods with
 * [@Test][org.junit.jupiter.api.Test] annotations directly.
 */
abstract class KofixtureAnnotationSpec : AnnotationSpec(), KofixtureTest {
    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}
