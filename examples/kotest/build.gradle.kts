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
    testImplementation("io.github.kofixture:kofixture-kotest:$kofixture_version")
    testImplementation("io.kotest:kotest-runner-junit5:6.1.10")
    testImplementation("io.kotest:kotest-assertions-core:6.1.10")
}

tasks.test {
    useJUnitPlatform()
}
