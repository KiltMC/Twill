package xyz.bluspring.twill.loader.neoforge

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.IBindingsProvider
import xyz.bluspring.twill.api.EventBuses

object TwillBindings : IBindingsProvider {
    override fun getGameBus(): IEventBus = EventBuses.GAME
}
