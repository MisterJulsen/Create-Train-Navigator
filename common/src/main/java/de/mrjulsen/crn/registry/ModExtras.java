package de.mrjulsen.crn.registry;

import com.tterrag.registrate.util.entry.RegistryEntry;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;

public class ModExtras {
                
    public static final RegistryEntry<AdvancedDisplaySource> ADVANCED_DISPLAY_SOURCE = CRNPlatformSpecific.registerDisplaySource();
            
    public static final RegistryEntry<AdvancedDisplayTarget> ADVANCED_DISPLAY_BOARD_TARGET = CreateRailwaysNavigator.REGISTRATE.displayTarget("advanced_display", AdvancedDisplayTarget::new)
        .register();

    public static void init() {
    }
}
