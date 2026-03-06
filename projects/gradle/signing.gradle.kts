apply(plugin = "signing")

// Signing is only active during release builds (IS_RELEASE=true env var).
// TODO: Before first release, configure these env vars as GitHub Actions secrets:
//   SIGNING_KEY_ID   — last 8 chars of your PGP key fingerprint
//   SIGNING_KEY      — armored private key block (gpg --armor --export-secret-keys <id>)
//   SIGNING_PASSWORD — passphrase protecting the key

fun isReleaseBuild(): Boolean = System.getenv("IS_RELEASE") == "true"

fun getSigningKeyId(): String =
    findProperty("SIGNING_KEY_ID")?.toString() ?: System.getenv("SIGNING_KEY_ID") ?: ""

fun getSigningKey(): String =
    findProperty("SIGNING_KEY")?.toString() ?: System.getenv("SIGNING_KEY") ?: ""

fun getSigningPassword(): String =
    findProperty("SIGNING_PASSWORD")?.toString() ?: System.getenv("SIGNING_PASSWORD") ?: ""

if (isReleaseBuild()) {
    tasks.withType<PublishToMavenLocal>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
    tasks.matching { it.name.endsWith("ToNmcpRepository") }.configureEach {
        dependsOn(tasks.withType<Sign>())
    }

    configure<SigningExtension> {
        useInMemoryPgpKeys(getSigningKeyId(), getSigningKey(), getSigningPassword())
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
