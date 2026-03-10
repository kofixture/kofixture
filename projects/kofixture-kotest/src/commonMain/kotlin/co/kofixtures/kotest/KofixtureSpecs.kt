package co.kofixtures.kotest

import co.kofixtures.core.KofixtureContext
import co.kofixtures.core.KofixtureTest
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.WordSpec

// Each spec style is an independent abstract class — Kotlin does not support multiple class
// inheritance, so beforeSpec/afterSpec cannot be de-duplicated via a shared base class.
// The logic is intentionally kept simple (2 lines each) to minimise maintenance risk.
// Note: KofixtureAnnotationSpec lives in jvmMain — AnnotationSpec is JVM-only in Kotest.

/** [BehaviorSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureBehaviorSpec(
    body: KofixtureBehaviorSpec.() -> Unit = {},
) : BehaviorSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [DescribeSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureDescribeSpec(
    body: KofixtureDescribeSpec.() -> Unit = {},
) : DescribeSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [ExpectSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureExpectSpec(
    body: KofixtureExpectSpec.() -> Unit = {},
) : ExpectSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [FeatureSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureFeatureSpec(
    body: KofixtureFeatureSpec.() -> Unit = {},
) : FeatureSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [FreeSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureFreeSpec(
    body: KofixtureFreeSpec.() -> Unit = {},
) : FreeSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/**
 * [FunSpec] base class that integrates [KofixtureTest].
 *
 * The constructor lambda runs with `KofixtureFunSpec` as receiver, so
 * [sample][co.kofixtures.core.KofixtureTest.sample],
 * [generator][co.kofixtures.core.KofixtureTest.generator] delegates are available directly:
 *
 * ```kotlin
 * class MyTest : KofixtureFunSpec({
 *     val user by sample<User> { name = "Alice" }
 *     test("name") { user.name shouldBe "Alice" }
 * }) {
 *     override val fixtureModules = listOf(myFixtures)
 * }
 * ```
 *
 * Registry lifecycle is managed automatically via [beforeSpec]/[afterSpec].
 * If `KofixtureListener` is also installed globally, double-calling is harmless.
 */
abstract class KofixtureFunSpec(
    body: KofixtureFunSpec.() -> Unit = {},
) : FunSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [ShouldSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureShouldSpec(
    body: KofixtureShouldSpec.() -> Unit = {},
) : ShouldSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [StringSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureStringSpec(
    body: KofixtureStringSpec.() -> Unit = {},
) : StringSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}

/** [WordSpec] base class that integrates [KofixtureTest]. See [KofixtureFunSpec] for usage. */
abstract class KofixtureWordSpec(
    body: KofixtureWordSpec.() -> Unit = {},
) : WordSpec(), KofixtureTest {
    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        buildRegistry()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        KofixtureContext.releaseFor(this)
    }
}
