package xyz.bluspring.twill.loader

import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.twill.loader.knit.NeoForgeMod

/**
 * This is mainly for Kilt's usage. Do not supply your own override.
 */
interface TwillOverrides {
    companion object {
        @JvmStatic
        var instance: TwillOverrides = object : TwillOverrides {}
    }

    val hasLaunchOverride: Boolean
        get() = false

    val mods: Collection<NeoForgeMod>
        get() = TwillLoader.instance.mods

    fun modExistsNatively(id: String): Boolean {
        return false
    }

    fun getNativeModId(dependencyId: String): String? {
        return null
    }

    suspend fun tryRemapMods(definitions: Collection<ModDefinition>) {}

    fun detectModSpecificEasterEggs(containers: Collection<NeoForgeMod>) {}
    fun finishModScanning() {}

    fun tryMakeActive(mod: NeoForgeMod): Boolean {
        return false
    }

    fun preInitialize() {}

    fun loadMods() {}
}
