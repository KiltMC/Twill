package xyz.bluspring.twill.loader.neoforge

import net.minecraft.CrashReport
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoader
import net.neoforged.fml.ModWorkManager
import net.neoforged.fml.config.ConfigTracker
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.*
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.fml.startup.FatalErrorReporting
import xyz.bluspring.twill.api.EventBuses
import java.nio.file.Path

// a modularized version of https://github.com/neoforged/NeoForge/blob/26.2.x/src/main/java/net/neoforged/neoforge/internal/CommonModLoader.java
abstract class NeoForgeModLoader {
    open fun doBeginTask() {}
    open fun doRegistrationTask() {}
    open fun doNetworkRegistryTask() {}

    fun begin(periodicTask: Runnable, datagen: Boolean) {
        ModLoader.gatherAndInitializeMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), periodicTask)

        this.doBeginTask()

        if (!datagen) {
            ModLoader.runInitTask("Config loading", ModWorkManager.syncExecutor(), periodicTask) {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, FMLPaths.CONFIGDIR.get())
                }

                ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get())
            }
        }

        EventBuses.GAME.start()
    }

    fun load(periodicTask: Runnable) {
        val syncExecutor = ModWorkManager.syncExecutor()
        val parallelExecutor = ModWorkManager.parallelExecutor()

        ModLoader.dispatchParallelEvent("Common setup", syncExecutor, parallelExecutor, periodicTask) { container, deferredWorkQueue ->
            FMLCommonSetupEvent(container, deferredWorkQueue)
        }

        ModLoader.dispatchParallelEvent("Sided setup", syncExecutor, parallelExecutor, periodicTask) { container, deferredWorkQueue ->
            if (FMLEnvironment.getDist().isClient)
                FMLClientSetupEvent(container, deferredWorkQueue)
            else
                FMLDedicatedServerSetupEvent(container, deferredWorkQueue)
        }

        this.doRegistrationTask()
//        ModLoader.runInitTask("Registration events", syncExecutor, periodicTask, RegistrationEvents::init)

        ModLoader.dispatchParallelEvent("Enqueue IMC", syncExecutor, parallelExecutor, periodicTask) { container, deferredWorkQueue ->
            InterModEnqueueEvent(container, deferredWorkQueue)
        }

        ModLoader.dispatchParallelEvent("Process IMC", syncExecutor, parallelExecutor, periodicTask) { container, deferredWorkQueue ->
            InterModProcessEvent(container, deferredWorkQueue)
        }

        ModLoader.dispatchParallelEvent("Complete loading of ${ModList.get().size()} mods", syncExecutor, parallelExecutor, periodicTask) { container, deferredWorkQueue ->
            FMLLoadCompleteEvent(container, deferredWorkQueue)
        }

        this.doNetworkRegistryTask()
//        ModLoader.runInitTask("Network registry lock", syncExecutor, periodicTask, NetworkRegistry::setup)
    }

    fun reportFatalError(error: Throwable, gameDir: Path, report: CrashReport) {
        val logFile = gameDir.resolve("logs", "latest.log")
        FatalErrorReporting.reportFatalError(error, gameDir, logFile, report.saveFile)
    }
}
