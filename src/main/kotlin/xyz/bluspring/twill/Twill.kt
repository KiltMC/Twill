package xyz.bluspring.twill

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Twill : ModInitializer {
    override fun onInitialize() {
    }

    companion object {
        const val MOD_ID = "twill"
        @JvmStatic val logger: Logger = LoggerFactory.getLogger("Twill")
    }
}
