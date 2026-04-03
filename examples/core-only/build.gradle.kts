plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val kofixture_version: String by project

dependencies {
    testImplementation("io.github.kofixture:kofixture-core:$kofixture_version")
    testImplementation("io.kotest:kotest-runner-junit5:6.1.6")
    testImplementation("io.kotest:kotest-assertions-core:6.1.6")
}

tasks.test {
    useJUnitPlatform()
}
