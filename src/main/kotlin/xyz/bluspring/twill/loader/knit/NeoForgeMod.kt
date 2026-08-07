package xyz.bluspring.twill.loader.knit

import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.locating.IModFile
import xyz.bluspring.knit.loader.mod.KnitMod
import xyz.bluspring.knit.loader.mod.ModDefinition

class NeoForgeMod(definition: ModDefinition) : KnitMod(definition) {
    val modFile: IModFile = definition.additionalData["file"] as IModFile
    val modFileInfo: IModInfo = definition.additionalData["info"] as IModInfo
}
