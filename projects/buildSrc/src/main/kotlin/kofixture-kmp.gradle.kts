import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    js(IR) {
        nodejs()
    }

    linuxX64()
    macosX64()
    macosArm64()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Native and JS tests run in dedicated CI jobs; disable locally for speed
tasks.withType<KotlinNativeTest>().configureEach {
    enabled = false
}

tasks.withType<KotlinJsTest>().configureEach {
    enabled = false
}
