enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kofixture"

include(
    ":kofixture-core",
    ":kofixture-kotest-arb",
    ":kofixture-kotest",
    ":kofixture-ksp",
)
