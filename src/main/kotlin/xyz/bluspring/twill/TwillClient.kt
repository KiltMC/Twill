package xyz.bluspring.twill

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import xyz.bluspring.twill.loader.TwillLoader
import xyz.bluspring.twill.loader.TwillOverrides

//? if <= 26.3 {
import net.minecraft.client.gui.components.debug.DebugScreenEntries
//? } else {
/*import net.minecraft.client.core.registries.BuiltInClientRegistries
import net.minecraft.client.gui.components.debug.DebugColumn
import net.minecraft.client.gui.components.debug.DebugGroup
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.neoforged.fml.classloading.transformation.ClassTransformStatistics
*///? }

class TwillClient : ClientModInitializer {
    private fun register(name: String, entry: DebugScreenEntry) {
        //? if <= 26.3 {
        DebugScreenEntries.register(id(name), entry)
        //? } else {
        /*Registry.register(BuiltInClientRegistries.DEBUG_SCREEN_ENTRY, id(name), entry)
        *///? }
    }

    private val twillVersion = FabricLoader.getInstance().getModContainer(Twill.MOD_ID).orElseThrow().metadata.version.friendlyString

    //? if <= 26.3 {
    private val debugGroup = id("info")
    //? } else {
    /*private val debugGroup = DebugGroup.Builder.titled(Component.literal("Twill Loader"))
        .withPreferredColumn(DebugColumn.Side.RIGHT)
        .build()
    *///? }

    private fun determineVersionColor(version: String): String {
        return if (version.endsWith("-local"))
            "§c"
        else if (version.contains("build."))
            "§6"
        else
            "§b"
    }

    override fun onInitializeClient() {
        register("info", object : DebugScreenEntry {
            override fun display(displayer: DebugScreenDisplayer, serverOrClientLevel: Level?, clientChunk: LevelChunk?, serverChunk: LevelChunk?) {
                val version = twillVersion
                val color = determineVersionColor(version)

                //? if <= 26.3 {
                displayer.addToGroup(debugGroup, "Twill Loader ${color}v$version")

                if (TwillOverrides.instance.overrideName != null) {
                    val version = TwillOverrides.instance.overrideVersion
                    if (version != null) {
                        displayer.addToGroup(debugGroup, "- via ${TwillOverrides.instance.overrideName} ${determineVersionColor(version)}v${version}")
                    } else {
                        displayer.addToGroup(debugGroup, "- via ${TwillOverrides.instance.overrideName}")
                    }
                }

                displayer.addToGroup(debugGroup, "- ${TwillLoader.instance.mods.size} mods loaded")
                //? } else {
                /*displayer.addFactToGroup(debugGroup, "Version") {
                    it.text("${color}$version")
                }
                if (TwillOverrides.instance.overrideName != null) {
                    displayer.addFactToGroup(debugGroup, "Provider") {
                        val version = TwillOverrides.instance.overrideVersion
                        if (version != null) {
                            it.text("${TwillOverrides.instance.overrideName} ${determineVersionColor(version)}v${version}")
                        } else {
                            it.text(TwillOverrides.instance.overrideName!!)
                        }
                    }
                }
                displayer.addFactToGroup(debugGroup, "FML") {
                    it.value(TwillConstants.FML_VERSION)
                }
                displayer.addFactToGroup(debugGroup, "Mods") {
                    it.value(TwillLoader.instance.mods.size)
                }
                displayer.addFactToGroup(debugGroup, "Classes") {
                    it.value(ClassTransformStatistics.getTransformationSummary())
                }
                *///? }
            }

            override fun isAllowed(reducedDebugInfo: Boolean): Boolean {
                return true
            }
        })
    }

    companion object {
        @JvmStatic
        fun id(name: String): Identifier {
            return Identifier.fromNamespaceAndPath(Twill.MOD_ID, name)
        }
    }
}
