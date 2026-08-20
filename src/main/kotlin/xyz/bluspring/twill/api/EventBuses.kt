package xyz.bluspring.twill.api

import net.neoforged.bus.api.IEventBus
import xyz.bluspring.twill.loader.TwillOverrides

object EventBuses {
    @JvmField val GAME: IEventBus = TwillOverrides.instance.gameBus
}
