import io.gitlab.arturbosch.detekt.Detekt

plugins {
    // kotlin-multiplatform and kotlin-jvm are applied per-module via the kofixture-kmp convention
    // plugin (buildSrc). Declaring them here would conflict with the buildSrc classpath.
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.kotlinBinary)
    alias(libs.plugins.nmcp)
}

// ── Spotless ─────────────────────────────────────────────────────────────────
// ktlint rule overrides live in .editorconfig at the repo root — no duplication here.
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt", "**/generated/**/*.kt")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}

// ── Detekt ────────────────────────────────────────────────────────────────────
detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom(
        fileTree(rootDir) {
            include("**/*.kt")
            exclude("**/build/**", "**/generated/**")
        },
    )
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
    jvmTarget = "21"
}

// ── Group / version + Dokka ───────────────────────────────────────────────────
allprojects {
    group = "io.github.kofixture" // TODO: confirm namespace with owner
    version = findProperty("kofixture.version") as String

    apply(plugin = "org.jetbrains.dokka")
}

// ── Binary Compatibility Validator ────────────────────────────────────────────
apiValidation {
    ignoredProjects.add("kofixture-ksp") // processor, not a stable library API
}

// ── NMCP (Maven Central Publishing) ──────────────────────────────────────────
// TODO: Before first release, add GitHub Secrets:
//   OSSRH_USERNAME, OSSRH_PASSWORD, SIGNING_KEY_ID, SIGNING_KEY, SIGNING_PASSWORD
fun getRepoUsername(): String = findProperty("OSSRH_USERNAME")?.toString() ?: System.getenv("OSSRH_USERNAME") ?: ""

fun getRepoPassword(): String = findProperty("OSSRH_PASSWORD")?.toString() ?: System.getenv("OSSRH_PASSWORD") ?: ""

nmcpAggregation {
    centralPortal {
        username.set(getRepoUsername())
        password.set(getRepoPassword())
        publishingType = "USER_MANAGED"
    }
    publishAllProjectsProbablyBreakingProjectIsolation()
}
