pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

// can't use libs.versions.toml for this - https://github.com/gradle/gradle/issues/36437
// make sure to update it there too tho.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
    id("dev.kikugie.stonecutter") version "0.9.+" // https://stonecutter.kikugie.dev/
}

val supportedVersions = listOf("26.1.2", "26.2")

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true

    create(rootProject) {
        versions(supportedVersions)
        vcsVersion = "26.1.2"
    }
}

includeBuild("knit-loader") {
    dependencySubstitution {
        substitute(module("xyz.bluspring.knit-loader:knit-loader-fabric"))
            .using(project(":fabric"))

        substitute(module("xyz.bluspring.knit-loader:knit-loader-quilt"))
            .using(project(":quilt"))
    }
}

rootProject.name = "Twill"
