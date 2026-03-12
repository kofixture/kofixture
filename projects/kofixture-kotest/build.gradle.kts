plugins {
    id("kofixture-kmp")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kofixture-core"))
            api(project(":kofixture-kotest-arb"))
            api(libs.kotest.framework.engine)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

apply(from = file("../gradle/publish.gradle.kts"))
