import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI
import java.util.*

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.fletching.table)
    `maven-publish`
    alias(libs.plugins.mod.publish)
}

val fmlVersion = property("fml") as String

version = "${createVersion()}+${stonecutter.current.version}${getVersionMetadata()}"
group = rootProject.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

stonecutter {
    dependencies["fml"] = fmlVersion
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

    if (stonecutter.eval(fmlVersion, ">=12")) {
        api(include("net.neoforged:JarJarFileSystems:${fmlProperties.getProperty("jarjar_version")}")!!)
    }

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

tasks.publish {
    finalizedBy("publishMods")
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
        maven("https://mvn.devos.one/releases") {
            name = "devOS"
            credentials {
                username = System.getenv()["MAVEN_USER"]
                password = System.getenv()["MAVEN_PASS"]
            }
        }
    }
}

tasks {
    project.extensions.configure<ModPublishExtension>("publishMods") {
        file = project.tasks.named<Jar>("jar").get().archiveFile
        displayName = "Twill v${project.version} (MC ${stonecutter.current.version})"
        version = project.version as String
        changelog = System.getenv("RELEASE_DESCRIPTION") ?: ""
        type = ReleaseType.STABLE
        modLoaders.add("fabric")

        dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
            || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

        modrinth {
            projectId = project.property("publishing.modrinth").toString()
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            minecraftVersions.addAll((property("supported_versions") as String).split(","))

            requires("fabric-api", "fabric-language-kotlin")
            embeds("modmenu-badges-lib")
        }

        curseforge {
            projectId = project.property("publishing.curseforge").toString()
            client = true
            server = true
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            minecraftVersions.addAll((property("supported_versions") as String).split(","))

            requires("fabric-api", "fabric-language-kotlin")
            embeds("modmenu-badges-lib")
        }
    }

    named("publishMavenJavaPublicationToDevOSRepository") {
        onlyIf {
            val group = project.property("maven_group") as String
            val artifactId = project.property("archives_base_name") as String
            val version = project.version.toString()

            try {
                val connection = URI.create("https://mvn.devos.one/releases/${group.replace(".", "/")}/${artifactId}/${version}/${artifactId}-${version}.jar").toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                connection.responseCode != 200
            } catch (_: Exception) {
                false
            }
        }
    }

    named("publishCurseforge") {
        onlyIf {
            try {
                // this is how CurseForge has it set up? seriously?
                val connection = URI.create("https://minecraft.curseforge.com/api/maven/twill/Twill/${version}/Twill-${version}.jar")
                    .toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                connection.responseCode != 200
            } catch (_: Exception) {
                false
            }
        }
    }

    named("publishModrinth") {
        onlyIf {
            try {
                val connection = URI.create("https://api.modrinth.com/v2/project/twill/version/${version}").toURL()
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                connection.responseCode != 200
            } catch (_: Exception) {
                false
            }
        }
    }
}

fun isRelease(): Boolean {
    return rootProject.hasProperty("build.release")
}

fun createVersion(): String {
    var newestVersion = "1.0.0"

    for (tag in try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "tag"), emptyArray(), rootProject.projectDir)
        process.inputReader().readLines()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }) {
        // Select the highest increment available.
        if (stonecutter.eval(tag.trim().removePrefix("v"), ">$newestVersion")) {
            newestVersion = tag.trim().removePrefix("v")
        }
    }

    // Bump the version accordingly
    if (!isRelease()) {
        val components = newestVersion.split(".")
        val patchVersion = components[2].toInt() + 1
        newestVersion = "${components[0]}.${components[1]}.$patchVersion"
    }

    val version = newestVersion

    if (isRelease() && System.getenv("GITHUB_TAG") != null) {
        val tag = System.getenv("GITHUB_TAG")
        if (tag.lowercase().removePrefix("v") != version)
            throw IllegalStateException("The tag created doesn't match the increment version! Are you incrementing it correctly? ($version != ${tag.lowercase().removePrefix("v")})")
    }

    return version
}

fun getVersionMetadata(): String {
    if (isRelease())
        return ""

    val commitHash = System.getenv("GITHUB_SHA") ?: (try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD"), emptyArray(), rootProject.projectDir)
        process.inputReader().readLine().trim()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    })

    return "-build.${commitHash.subSequence(0, 6)}${
        if (System.getenv("GITHUB_RUN_NUMBER") == null) 
            "-local" 
        else if (!isRelease()) 
            "-nightly" 
        else 
            ""
    }"
}
