apply(plugin = "maven-publish")

configure<PublishingExtension> {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Kofixture")
                description.set("Kotlin Multiplatform test fixture generation library")
                url.set("https://github.com/kofixture/kofixture") // TODO: update to real URL

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    url.set("https://github.com/kofixture/kofixture") // TODO: update to real URL
                    connection.set("scm:git:https://github.com/kofixture/kofixture.git")
                }

                developers {
                    developer {
                        // TODO: add real developer name and contact
                        name.set("Kofixture Contributors")
                    }
                }
            }
        }
    }
}

apply(from = rootProject.file("gradle/signing.gradle.kts"))
