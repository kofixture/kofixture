rootProject.name = "kofixture-examples"

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

includeBuild("../projects")

include(
    ":core-only",
    ":kotest",
)
