package de.mrjulsen.crn.compat.tramways;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.SpeedLimitProviderRegistry;
import net.minecraft.resources.ResourceLocation;

public final class TramwaysCompat {

    private TramwaysCompat() {}

    public static void init() {
        SpeedLimitProviderRegistry.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "tramways"), new TramwaysSpeedLimitProvider());
    }
}
