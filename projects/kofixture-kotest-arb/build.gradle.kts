plugins {
    id("kofixture-kmp")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kofixture-core"))
            api(libs.kotest.property)
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

apply(from = file("../gradle/publish.gradle.kts"))
