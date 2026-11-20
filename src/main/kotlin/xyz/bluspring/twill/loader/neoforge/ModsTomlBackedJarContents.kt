package xyz.bluspring.twill.loader.neoforge

import net.neoforged.fml.jarcontents.JarContents
import net.neoforged.fml.jarcontents.JarResource
import net.neoforged.fml.jarcontents.JarResourceAttributes
import net.neoforged.fml.jarcontents.JarResourceVisitor
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.jar.Manifest

class ModsTomlBackedJarContents(
    val paths: List<Path>,
    val tomlUri: URI?,
    val tomlStream: () -> InputStream,
) : JarContents {
    private val backing = JarContents.ofPaths(this.paths)

    override fun getChecksum(): Optional<String> = this.backing.checksum
    override fun getPrimaryPath(): Path? = this.backing.primaryPath
    override fun getContentRoots(): Collection<Path?>? = this.backing.contentRoots

    override fun get(relativePath: String?): JarResource? {
        if (relativePath == "META-INF/neoforge.mods.toml") {
            return object : JarResource {
                override fun open(): InputStream = tomlStream()

                override fun attributes(): JarResourceAttributes? {
                    val size = tomlStream().use { it.readAllBytes().size.toLong() }
                    return JarResourceAttributes(FileTime.fromMillis(System.currentTimeMillis()), size)
                }

                override fun retain(): JarResource {
                    return this
                }
            }
        }

        return this.backing.get(relativePath)
    }

    override fun findFile(relativePath: String?): Optional<URI>? {
        if (relativePath == "META-INF/neoforge.mods.toml")
            return if (tomlUri != null)
                Optional.of(tomlUri)
            else
                this.backing.findFile("META-INF/kilt_neoforge.mods.toml")

        return this.backing.findFile(relativePath)
    }

    override fun openFile(relativePath: String?): InputStream? {
        if (relativePath == "META-INF/neoforge.mods.toml")
            return tomlStream()

        return this.backing.openFile(relativePath)
    }

    override fun containsFile(relativePath: String?): Boolean {
        if (relativePath == "META-INF/neoforge.mods.toml") {
            return true
        }

        return this.backing.containsFile(relativePath)
    }

    override fun getManifest(): Manifest? = this.backing.manifest
    override fun visitContent(startingFolder: String?, visitor: JarResourceVisitor?) = this.backing.visitContent(startingFolder, visitor)
    override fun close() = this.backing.close()
}
