plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.kotlin.reflect)

    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

apply(from = file("../gradle/publish.gradle.kts"))

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
