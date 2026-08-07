package xyz.bluspring.twill.loader.neoforge.server

import net.neoforged.fml.ModLoadingException
import xyz.bluspring.twill.loader.neoforge.NeoForgeModLoader

open class NeoForgeServerModLoader : NeoForgeModLoader() {
    fun load(gameTestServer: Boolean) {
        try {
            this.begin({}, false)
            this.load {}
        } catch (error: ModLoadingException) {
            
        }
    }
}
