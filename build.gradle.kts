import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.fletching.table)
    `maven-publish`
}

val fmlVersion = property("fml") as String

version = "${rootProject.property("mod_version")}+${stonecutter.current.version}"
group = rootProject.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

sourceSets {
    getByName("main") {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")

        java.srcDir(rootProject.file("fml/v$fmlVersion/loader/src/main/java"))

        resources.srcDir("src/main/resources")
        resources.srcDir(rootProject.file("fml/v$fmlVersion/loader/src/main/resources"))
    }
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)

    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")
    implementation(libs.eventbus)
    include(libs.eventbus)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", stonecutter.current.version)
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to stonecutter.current.version,
            "loader_version" to libs.versions.fabric.loader.get(),
            "kotlin_loader_version" to libs.versions.fabric.kotlin.get(),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
