import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

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
    maven("https://mvn.devos.one/releases")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.florianreuth.de/snapshots")
    maven("https://maven.bawnorton.com/releases")
    maven("https://maven.enjarai.dev/mirrors")
    maven("https://api.modrinth.com/maven")
    maven("https://jitpack.io")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${findProperty("minecraft_override") ?: stonecutter.current.version}")
    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)

    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")

    val fmlProperties = Properties()
    fmlProperties.load(rootProject.file("fml/v$fmlVersion/gradle.properties").inputStream())
    api(include("net.neoforged:bus:${fmlProperties.getProperty("eventbus_version")}")!!)
    api(include("net.neoforged:accesstransformers:${fmlProperties.getProperty("accesstransformers_version")}")!!)
    api(include("net.neoforged:mergetool:${fmlProperties.getProperty("mergetool_version")}")!!)
    api(include("net.neoforged:JarJarSelector:${fmlProperties.getProperty("jarjar_version")}")!!)
    api(include("net.neoforged:JarJarMetadata:${fmlProperties.getProperty("jarjar_version")}")!!)
    api(include("org.apache.maven:maven-artifact:${fmlProperties.getProperty("apache_maven_artifact_version")}")!!)
    api(include("com.electronwill.night-config:core:${fmlProperties.getProperty("nightconfig_version")}")!!)
    api(include("com.electronwill.night-config:toml:${fmlProperties.getProperty("nightconfig_version")}")!!)

    try {
        api(include(fletchingTable.modrinth("modmenu-badges-lib", stonecutter.current.version, "fabric"))!!)
    } catch (_: Throwable) {}

    implementation(libs.massasmer)
    include(libs.massasmer)

    api(libs.knit.loader.fabric)
    include(libs.bundles.knit.loader)

    api(libs.bundles.tiny.codecs)
    include(libs.bundles.tiny.codecs)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", stonecutter.current.version)
    filteringCharset = "UTF-8"

    exclude("log4j2.component.properties") // We don't need this

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("mc_version_range")!!,
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
