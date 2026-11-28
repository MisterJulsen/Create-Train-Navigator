/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.compat.tramways;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.speed.SpeedLimitProviderRegistry;
import net.minecraft.resources.ResourceLocation;

public final class TramwaysCompat {

    private TramwaysCompat() {}

    public static void init() {
        SpeedLimitProviderRegistry.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "tramways"), new TramwaysSpeedLimitProvider());
    }
}
