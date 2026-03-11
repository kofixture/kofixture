// The library modules are wired via composite build in settings.gradle.kts —
// no publishToMavenLocal step needed.

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val kofixture_version: String by project

dependencies {
    kspTest("io.github.kofixture:kofixture-ksp:$kofixture_version")
    testImplementation("io.github.kofixture:kofixture-kotest:$kofixture_version")
    testImplementation("io.kotest:kotest-runner-junit5:6.1.6")
}

tasks.test {
    useJUnitPlatform()
}
