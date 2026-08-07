package xyz.bluspring.twill.api.util

import net.fabricmc.api.EnvType
import net.neoforged.api.distmarker.Dist

object PlatformConversionUtils {
    @JvmStatic
    val Dist.asFabric: EnvType
        get() = when (this) {
            Dist.CLIENT -> EnvType.CLIENT
            Dist.DEDICATED_SERVER -> EnvType.SERVER
        }

    @JvmStatic
    val EnvType.asNeoForge: Dist
        get() = when (this) {
            EnvType.CLIENT -> Dist.CLIENT
            EnvType.SERVER -> Dist.DEDICATED_SERVER
        }
}
