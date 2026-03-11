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
            url.set("https://github.com/kofixture/kofixture")

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            scm {
                connection.set("scm:git:https://github.com/kofixture/kofixture.git")
                developerConnection.set("scm:git:ssh://git@github.com/kofixture/kofixture.git")
                url.set("https://github.com/kofixture/kofixture")
            }

            developers {
                developer {
                    name.set("Kofixture Contributors")
                    id.set("arkadiusz")
                    name.set("Arkadiusz Szast")
                    email.set("szast.arkadiusz@pm.me")
                    organization.set("Kofixture")
                    organizationUrl.set("https://github.com/kofixture")
                }
            }
        }
    }
}

apply(from = rootProject.file("gradle/signing.gradle.kts"))
