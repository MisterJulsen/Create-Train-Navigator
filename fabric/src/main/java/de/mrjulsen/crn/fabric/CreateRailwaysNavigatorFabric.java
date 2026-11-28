/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.fabric;


import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.fabricmc.api.ModInitializer;

public class CreateRailwaysNavigatorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.init();
        CreateRailwaysNavigator.REGISTRATE.register();
    }
}
