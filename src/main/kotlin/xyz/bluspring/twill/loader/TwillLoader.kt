package xyz.bluspring.twill.loader

import com.google.gson.JsonParser
import fish.cichlidmc.tinyjson.TinyJson
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.neoforged.fml.classloading.transformation.ClassProcessorSet
import net.neoforged.fml.jarcontents.JarContents
import net.neoforged.fml.jarcontents.JarFileContents
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.LanguageProviderLoader
import net.neoforged.fml.loading.moddiscovery.ModDiscoverer
import net.neoforged.fml.loading.moddiscovery.ModFile
import net.neoforged.fml.loading.moddiscovery.ModFileParser
import net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader
import net.neoforged.fml.startup.InstrumentationHelper
import net.neoforged.fml.startup.StartupArgs
import net.neoforged.jarjar.selection.JarSelector
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.locating.IModFile
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.knit.loader.mod.ModDependency
import xyz.bluspring.knit.loader.mod.ModEnvironment
import xyz.bluspring.twill.Twill
import xyz.bluspring.twill.api.WrappedModContainerEntrypoint
import xyz.bluspring.twill.api.util.PlatformConversionUtils.asNeoForge
import xyz.bluspring.twill.loader.fabric.WrappedFabricModContainer
import xyz.bluspring.twill.loader.knit.NeoForgeMod
import xyz.bluspring.twill.loader.knit.NeoForgeModVersion
import xyz.bluspring.twill.loader.knit.NeoForgeVersionConstraint
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.*

open class TwillLoader : KnitModLoader<NeoForgeMod>("twill", "neoforge") {
    companion object {
        @JvmStatic
        lateinit var instance: TwillLoader

        lateinit var classProcessors: ClassProcessorSet
            private set
    }

    var config = TwillLoaderConfig()

    init {
        instance = this
        val loader = FabricLoader.getInstance()

        // Sanity check for determining if Fabric mods are bundling NeoForge classes for whatever reason
        for (container in loader.allMods) {
            // Ignore ourselves and whatever we know works correctly.
            if (container.metadata.id == "twill" || container.metadata.id == "kilt" ||
                // If the mod parent is Kilt, then it's probably safe.
                (container.containingMod.isPresent && (container.containingMod.orElseThrow().metadata.id == "twill" || container.containingMod.orElseThrow().metadata.id == "kilt"))
            )
                continue

            val path = container.findPath("net/neoforged/")

            if (path.isPresent && path.orElseThrow().isDirectory()) {
                Twill.logger.warn("Twill: Fabric mod ${container.metadata.name} (${container.metadata.id}) is likely repackaging NeoForge or FancyModLoader classes! This may lead to a game crash!")
            }
        }

        FMLLoader.`twill$create`(InstrumentationHelper.obtainInstrumentation(), StartupArgs(
            FabricLoader.getInstance().gameDir,
            FabricLoader.getInstance().environmentType != EnvType.CLIENT,
            FabricLoader.getInstance().environmentType.asNeoForge,
            false,
            arrayOf(
                *FabricLoader.getInstance().getLaunchArguments(true),
                "--fml.mcVersion", FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().metadata.version.friendlyString,
                "--fml.neoFormVersion", "none",
            ),
            setOf(),
            listOf(),
            this::class.java.classLoader,
        ), this::class.java.classLoader, !FabricLoader.getInstance().isDevelopmentEnvironment)
        this.loadConfig()
    }

    fun loadConfig() {
        if (TwillLoaderConfig.PATH.exists()) {
            try {
                TwillLoaderConfig.CODEC.decode(TinyJson.parse(TwillLoaderConfig.PATH.reader(options = arrayOf(
                    StandardOpenOption.READ))))
                    .ifPresentOrElse({
                        this.config = it
                    }, {
                        Twill.logger.error("An error occurred while trying to load the Twill loader config! Error: $it")
                    })
            } catch (e: Throwable) {
                Twill.logger.error("An error occurred while trying to load the Twill loader config!", e)
            }
        }
    }

    override fun getModDefinitions(path: Path): List<ModDefinition> {
        if (path.extension != "jar")
            return emptyList()

        // Load all mod definitions. This is recursive, and since we also need to handle JiJ, it's separated into another method.
        return loadModDefinitions(path)
    }

    private val IModInfo.DependencyType.asKnit: ModDependency.Type
        get() = when (this) {
            IModInfo.DependencyType.REQUIRED -> ModDependency.Type.REQUIRED
            IModInfo.DependencyType.OPTIONAL -> ModDependency.Type.OPTIONAL
            IModInfo.DependencyType.INCOMPATIBLE -> ModDependency.Type.INCOMPATIBLE
            IModInfo.DependencyType.DISCOURAGED -> ModDependency.Type.DISCOURAGED
        }

    private val IModInfo.DependencySide.asKnit: ModEnvironment
        get() = when (this) {
            IModInfo.DependencySide.CLIENT -> ModEnvironment.CLIENT
            IModInfo.DependencySide.SERVER -> ModEnvironment.SERVER
            IModInfo.DependencySide.BOTH -> ModEnvironment.BOTH
        }

    private val IModFile.mixinConfigs: List<ModFileParser.MixinConfig>
        get() = (this as? ModFile)?.mixinConfigs ?: emptyList()

    private fun IModFile.asKnitDefinitions(parent: IModFile?): List<ModDefinition> {
        if (this.type != IModFile.Type.MOD && this.modInfos.isEmpty()) {
            return listOf(
                ModDefinition(this.filePath, this.id, this.fileName, version = NeoForgeModVersion(DefaultArtifactVersion(this.modFileInfo.versionString() ?: "0.0.0-unknown")), license = this.modFileInfo.license,
                    additionalData = mapOf(
                        "file" to this,
                    ),
                    loaderCustomData = mapOf(
                        "badges" to listOf("library"), // force ModMenu to hide this
                    )
                )
            )
        }

        return this.modInfos.map { info ->
            ModDefinition(this.filePath, info.modId, info.displayName, info.description,
                NeoForgeModVersion(info.version),
                (this.modFileInfo.config.getConfigElement<String>("authors")
                    .orElse("") ?: "").replace("\r\n", "\n").split(",")
                    .map { it.trim() },
                this.modFileInfo.license,
                info.dependencies.map { dep ->
                    ModDependency(dep.modId, NeoForgeVersionConstraint(dep.versionRange), dep.type.asKnit, dep.side.asKnit)
                },
                this.mixinConfigs
                    .map { mixinConfig ->
                        ModDefinition.MixinConfig(
                            mixinConfig.config,
                            ModEnvironment.BOTH,
                            mixinConfig.behaviorVersion?.run { NeoForgeModVersion(this) },
                            mixinConfig.requiredMods,
                        )
                    },
                parent?.id,
                info.logoFile.orElse("") ?: "",
                ModEnvironment.BOTH,

                additionalData = mapOf(
                    "info" to info,
                    "file" to this,
                ),

                loaderCustomData = mapOf(
                    "mcb" to listOf<Map<String, Any>>(
                        // https://syorito-hatsuki.github.io/modmenu-badges-lib/
                        mapOf(
                            "name" to "NeoForge",
                            "labelColor" to argb(255, 255, 255),
                            "outlineColor" to argb(207, 128, 55),
                            "fillColor" to argb(136, 60, 18),
                        )
                    )
                ),
            )
        }
    }

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int {
        // 0xFF_FF_FF_FF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private val loadedModIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override fun modExistsNatively(id: String): Boolean {
        if (this.loadedModIds.contains(id))
            return false

        return super.modExistsNatively(id)
    }

    override fun getNativeModId(dependencyId: String, nativeLoaderName: String): String {
        val loader = FabricLoader.getInstance()
        if (loader.isModLoaded(dependencyId))
            return dependencyId

        // fun times.
        if (loader.isModLoaded(dependencyId.replace("_", "-")))
            return dependencyId.replace("_", "-")
        else if (loader.isModLoaded(dependencyId.replace("_", "")))
            return dependencyId.replace("_", "")

        return super.getNativeModId(dependencyId, nativeLoaderName)
    }

    private fun tryGetModFile(jar: JarContents, attributes: ModFileDiscoveryAttributes): IModFile? {
        var modFile = JarModsDotTomlModFileReader.createModFile(jar, attributes)

        if (modFile == null && attributes.parent != null) {
            modFile = IModFile.create(jar, JarModsDotTomlModFileReader::manifestParser, IModFile.Type.LIBRARY, attributes)
        }

        return modFile
    }

    private fun tryGetModFiles(jar: JarContents, attributes: ModFileDiscoveryAttributes): Collection<IModFile> {
        val modFile = tryGetModFile(jar, attributes)

        if (modFile != null) {
            val files = mutableListOf<IModFile>()
            files.add(modFile)

            val jarJar = JarSelector.detectAndSelect(listOf(modFile), { modFile, relativePath ->
                try {
                    Optional.ofNullable(modFile.contents.openFile(relativePath))
                } catch (e: Throwable) {
                    Twill.logger.error("Failed to load resource $relativePath from mod ${modFile.fileName}!", e)
                    Optional.empty()
                }
            }, { file, path ->
                if (!jijPath.exists())
                    jijPath.createDirectories()

                val tempFile = Files.createTempFile(jijPath, "_jij", ".tmp")
                val finalPath = try {
                    val checksum = JarInJarDependencyLocator.extractEmbeddedJarFile(file, path, tempFile)
                    val fileName = path.substring(path.lastIndexOf('/') + 1)
                    val final = jijPath.resolve("$checksum/$fileName")
                    if (!Files.isRegularFile(final))
                        JarInJarDependencyLocator.moveExtractedFileIntoPlace(tempFile, final)

                    final
                } finally {
                    Files.deleteIfExists(tempFile)
                }

                val jar = JarContents.ofPath(finalPath)
                Optional.ofNullable(tryGetModFile(jar, attributes.withParent(modFile)))
            }, { file ->
                if (file.modFileInfo == null)
                    file.fileName
                else if (file.modInfos.isEmpty())
                    "library:${file.id}"
                else
                    file.modInfos.joinToString { it.modId }
            }, { errors ->
                val error = RuntimeException("Failed to load JiJ data in mod file ${modFile.fileName}!")
                for (errorInfo in errors) {
                    error.addSuppressed(RuntimeException("${errorInfo.identifier()} -> ${errorInfo.failureReason()}"))
                }

                error
            })
            files.addAll(jarJar)

            return files
        }

        return emptyList()
    }

    private val jijPath = FabricLoader.getInstance().gameDir.resolve(".twill/extractedMods")

    override fun collectAdditionalModDefinitions(gameDir: Path): Collection<ModDefinition> {
        val discoveryResult = FMLLoader.getCurrent().runDiscovery()
        val definitions = discoveryResult.modFiles().flatMap { it.asKnitDefinitions(it.discoveryAttributes.parent) }

        if (discoveryResult.discoveryIssues.isNotEmpty())
            Twill.logger.error("Encountered issues while handling FML mod discovery!")

        for (issue in discoveryResult.discoveryIssues()) {
            Twill.logger.error("- $issue")
        }

        return definitions
    }

    protected open fun loadModDefinitions(path: Path, parent: IModFile? = null): List<ModDefinition> {
        if (path.extension != "jar")
            return emptyList()

        val jar = JarFileContents(path)
        val isTwillMod = if (jar.containsFile("fabric.mod.json")) {
            // Detect if it is a Twill stub mod
            val fmj = jar.get("fabric.mod.json")!!
            try {
                val json = JsonParser.parseReader(fmj.bufferedReader()).asJsonObject

                if (json.has("custom")) {
                    val customMeta = json.getAsJsonObject("custom")
                    customMeta.has("kilt:mod_stub") || customMeta.has("twill:mod_stub")
                } else false
            } catch (e: Throwable) {
                Twill.logger.error("Could not parse fabric.mod.json in mod ${path.name}! Assuming it is a Twill mod stub.", e)
                true
            }
        } else true

        // This mod isn't a Twill-supported mod JAR, we should skip it.
        if (!isTwillMod && parent == null)
            return emptyList()

        val definitions = mutableListOf<ModDefinition>()
        val attributes = ModFileDiscoveryAttributes.DEFAULT.withParent(parent)
        val modFiles = tryGetModFiles(jar, attributes)

        definitions.addAll(modFiles.flatMap { it.asKnitDefinitions(it.discoveryAttributes.parent) })

        for (definition in definitions) {
            this.loadedModIds.add(definition.id)
        }

        return definitions
    }

    override suspend fun createModContainers(definitions: Collection<ModDefinition>): Collection<NeoForgeMod> {
        val mods = definitions.map(::NeoForgeMod)
        val discovery = FMLLoader.getCurrent().`twill$setup`(ModDiscoverer.Result(
            mods.map { it.modFile as ModFile }, // apparently FML just does a direct cast. okay then.
            emptyList()
        ))
        classProcessors = FMLLoader.createClassProcessorSet(null, FMLLoader.getCurrent().LaunchContextAdapter(), discovery, null)
        return mods
    }

    override fun finishModScanning() {
        super.finishModScanning()

        // This process was moved from FMLLoader.create
        val fml = FMLLoader.getCurrent()
        fml.loadPlugins(fml.loadingModList.plugins)
        fml.languageProviderLoader = LanguageProviderLoader(fml.LaunchContextAdapter(), )
        for (file in fml.loadingModList.allModFiles) {
            (file as ModFile).identifyLanguage()
        }
    }

    open fun loadMods() {
        // Let's provide any Fabric mods with their wrapped container entrypoints
        for (container in FabricLoader.getInstance().getEntrypointContainers(WrappedModContainerEntrypoint.ENTRYPOINT, WrappedModContainerEntrypoint::class.java)) {
            container.entrypoint.onLoadModContainer(WrappedFabricModContainer.get(container.provider))
        }
    }
}
