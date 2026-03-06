plugins {
    id("kofixture-kmp")
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
        jvmMain.dependencies {
            implementation(libs.kotlin.reflect)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
        nativeMain.dependencies {
            implementation(libs.kotlin.reflect)
        }
    }
}

apply(from = file("../gradle/publish.gradle.kts"))
