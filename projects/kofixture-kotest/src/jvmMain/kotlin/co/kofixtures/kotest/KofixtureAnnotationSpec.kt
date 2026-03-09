package co.kofixtures.kotest

import co.kofixtures.core.KofixtureContext
import co.kofixtures.core.KofixtureTest
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec

// AnnotationSpec is JVM-only in Kotest; this wrapper is therefore also JVM-only.
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
