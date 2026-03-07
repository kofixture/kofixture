// Before building examples, publish the library to local Maven:
//   cd ../projects && ./install.sh

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
    testImplementation("io.kotest:kotest-runner-junit5:6.0.0.M4")
}

tasks.test {
    useJUnitPlatform()
}
