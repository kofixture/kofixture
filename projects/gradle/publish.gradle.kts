apply(plugin = "maven-publish")

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaGeneratePublicationJavadoc"))
    from(
        tasks.named("dokkaGeneratePublicationJavadoc").map {
            (it as org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask).outputDirectory
        },
    )
    archiveClassifier.set("javadoc")
}

configure<PublishingExtension> {
    publications.withType<MavenPublication> {
        artifact(dokkaJavadocJar)
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

apply(from = rootProject.file("gradle/signing.gradle.kts"))
