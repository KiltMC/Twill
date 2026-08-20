package xyz.bluspring.twill

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntries
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import xyz.bluspring.twill.loader.TwillLoader

class TwillClient : ClientModInitializer {
    override fun onInitializeClient() {
        DebugScreenEntries.register(id("version"), object : DebugScreenEntry {
            override fun display(displayer: DebugScreenDisplayer, serverOrClientLevel: Level?, clientChunk: LevelChunk?, serverChunk: LevelChunk?) {
                val version = FabricLoader.getInstance().getModContainer("twill")
                    .orElseThrow().metadata.version.friendlyString

                val color = if (version.endsWith("-local"))
                    "§c"
                else if (version.contains("+build."))
                    "§6"
                else
                    "§b"

                displayer.addToGroup(id("info"), "Twill Loader ${color}v$version")
            }

            override fun isAllowed(reducedDebugInfo: Boolean): Boolean {
                return true
            }
        })

        DebugScreenEntries.register(id("loaded_mods"), object : DebugScreenEntry {
            override fun display(displayer: DebugScreenDisplayer, serverOrClientLevel: Level?, clientChunk: LevelChunk?, serverChunk: LevelChunk?) {
                displayer.addToGroup(id("info"), "${TwillLoader.instance.mods.size} mods loaded")
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
