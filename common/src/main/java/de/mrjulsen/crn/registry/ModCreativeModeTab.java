/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(CreateRailwaysNavigator.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
        "createrailwaysnavigatortab",
        () -> CreativeTabRegistry.create(
                TextUtils.translate("itemGroup.createrailwaysnavigator.tab"),
                () -> new ItemStack(ModItems.NAVIGATOR.get())
        )
    );

    public static void setup() {
        CREATIVE_MODE_TABS.register();
    }
}