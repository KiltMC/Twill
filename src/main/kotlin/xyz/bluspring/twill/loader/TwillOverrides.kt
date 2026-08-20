package xyz.bluspring.twill.loader

import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.IModBusEvent
import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.twill.loader.knit.NeoForgeMod

/**
 * This is mainly for Kilt's usage. Do not supply your own override.
 */
interface TwillOverrides {
    companion object {
        @JvmStatic
        var instance: TwillOverrides = object : TwillOverrides {
            override val gameBus: IEventBus by lazy {
                BusBuilder.builder()
                    .startShutdown()
                    .classChecker { eventType ->
                        if (IModBusEvent::class.java.isAssignableFrom(eventType))
                            throw IllegalArgumentException("IModBusEvent events are not allowed on the common bus! Use a mod bus instead.")
                    }
                    .build()
            }
        }
    }

    val hasLaunchOverride: Boolean
        get() = false

    val gameBus: IEventBus

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
