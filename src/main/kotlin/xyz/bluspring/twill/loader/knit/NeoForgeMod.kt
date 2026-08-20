package xyz.bluspring.twill.loader.knit

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.locating.IModFile
import xyz.bluspring.knit.loader.mod.KnitMod
import xyz.bluspring.knit.loader.mod.ModDefinition

class NeoForgeMod(definition: ModDefinition) : KnitMod(definition) {
    val modFile: IModFile = definition.additionalData["file"] as IModFile
    val modInfo: IModInfo? = definition.additionalData["info"] as? IModInfo

    var shouldScan: Boolean = true
    val container: ModContainer by lazy {
        ModList.get().getModContainerById(this.definition.id).orElseThrow()
    }
    val eventBus: IEventBus by lazy {
        this.container.eventBus!!
    }
}
