package xyz.bluspring.twill

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
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
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import xyz.bluspring.twill.loader.TwillLoader

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

    override fun onInitializeClient() {
        register("info", object : DebugScreenEntry {
            override fun display(displayer: DebugScreenDisplayer, serverOrClientLevel: Level?, clientChunk: LevelChunk?, serverChunk: LevelChunk?) {
                val version = twillVersion

                val color = if (version.endsWith("-local"))
                    "§c"
                else if (version.contains("+build."))
                    "§6"
                else
                    "§b"

                //? if <= 26.3 {
                displayer.addToGroup(debugGroup, "Twill Loader ${color}v$version")
                displayer.addToGroup(debugGroup, "${TwillLoader.instance.mods.size} mods loaded")
                //? } else {
                /*displayer.addFactToGroup(debugGroup, "Version") {
                    it.text("${color}$version")
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
