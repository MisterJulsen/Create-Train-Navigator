package de.mrjulsen.crn.api.web;

import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.DragonLib;

/** Shared helpers for the station-stop web API on dedicated and integrated servers. */
public final class WebApiSupport {

    private WebApiSupport() {}

    public static long currentWorldTick() {
        return ModCommonEvents.getCurrentServer()
            .map(server -> server.overworld().getGameTime())
            .orElseGet(() -> {
                long dlTime = DragonLib.getCurrentWorldTime();
                return dlTime > 0 ? dlTime : 0L;
            });
    }
}
