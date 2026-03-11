plugins {
    kotlin("jvm") // version managed by buildSrc (kotlin-gradle-plugin on classpath)
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
    compileOnly(libs.ksp.api)
    implementation(project(":kofixture-core"))

    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(project(":kofixture-kotest-arb"))
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
