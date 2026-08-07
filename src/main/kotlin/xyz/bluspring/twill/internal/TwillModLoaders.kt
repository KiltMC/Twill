package xyz.bluspring.twill.internal

import xyz.bluspring.twill.loader.neoforge.client.NeoForgeClientModLoader
import xyz.bluspring.twill.loader.neoforge.server.NeoForgeServerModLoader

object TwillModLoaders {
    val clientModLoader: NeoForgeClientModLoader by lazy {
        NeoForgeClientModLoader()
    }
    
    val serverModLoader: NeoForgeServerModLoader by lazy {
        NeoForgeServerModLoader()
    }
}
