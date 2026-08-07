package xyz.bluspring.twill.api

import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.IModBusEvent

object EventBuses {
    @JvmField val GAME: IEventBus = BusBuilder.builder()
        .startShutdown()
        .classChecker { eventType ->
            if (IModBusEvent::class.java.isAssignableFrom(eventType))
                throw IllegalArgumentException("IModBusEvent events are not allowed on the common bus! Use a mod bus instead.")
        }
        .build()
}
