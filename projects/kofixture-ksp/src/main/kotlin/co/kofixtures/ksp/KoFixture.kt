package co.kofixtures.ksp

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KoFixture(
    val packages: Array<String> = [],
    val classes: Array<KClass<*>> = [],
    val moduleName: String = "",
)
