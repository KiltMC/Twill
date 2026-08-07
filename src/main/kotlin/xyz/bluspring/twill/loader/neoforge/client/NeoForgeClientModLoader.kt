package xyz.bluspring.twill.loader.neoforge.client

import net.minecraft.CrashReport
import net.minecraft.client.Minecraft
import net.minecraft.util.NativeModuleLister
import net.neoforged.fml.ModLoadingException
import org.apache.logging.log4j.LogManager
import xyz.bluspring.twill.loader.neoforge.NeoForgeModLoader

open class NeoForgeClientModLoader : NeoForgeModLoader() {
    fun begin() {
        Runtime.getRuntime().addShutdownHook(Thread(LogManager::shutdown))

        this.begin({}, false)
    }

    fun finish() {
        try {
            this.load {}
        } catch (e: ModLoadingException) {
            val gameDir = Minecraft.getInstance().gameDirectory
            val report = CrashReport.forThrowable(e, "stage")
            val category = report.addCategory("Finish mod loading (Twill/NeoForge)")
            NativeModuleLister.addCrashSection(category)

            Minecraft.getInstance().fillReport(report)
            Minecraft.saveReport(gameDir, report)

            this.reportFatalError(e, gameDir.toPath(), report)
        }
    }
}
