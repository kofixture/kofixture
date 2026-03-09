rootProject.name = "kofixture-examples"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Wire the library projects as a composite build so no publishToMavenLocal is needed.
// Gradle automatically substitutes the Maven coordinates below with the local source projects.
includeBuild("../projects") {
    dependencySubstitution {
        substitute(module("io.github.kofixture:kofixture-core")).using(project(":kofixture-core"))
        substitute(module("io.github.kofixture:kofixture-kotest-arb")).using(project(":kofixture-kotest-arb"))
        substitute(module("io.github.kofixture:kofixture-kotest")).using(project(":kofixture-kotest"))
        substitute(module("io.github.kofixture:kofixture-ksp")).using(project(":kofixture-ksp"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":app")
