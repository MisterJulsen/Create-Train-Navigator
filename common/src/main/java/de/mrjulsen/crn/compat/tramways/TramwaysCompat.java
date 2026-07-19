package de.mrjulsen.crn.compat.tramways;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.SpeedLimitProviderRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers CRN's Tramways speed-limit integration.
 * <p>
 * Only call {@link #init()} behind a {@code Platform.isModLoaded("tramways")} check - this class
 * and {@link TramwaysSpeedLimitProvider} reference Tramways types directly, so they must never be
 * loaded when the mod isn't present.
 */
public final class TramwaysCompat {

    private TramwaysCompat() {}

    public static void init() {
        SpeedLimitProviderRegistry.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "tramways"), new TramwaysSpeedLimitProvider());
    }
}
