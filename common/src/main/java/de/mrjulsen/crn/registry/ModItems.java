/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.registry;

import com.tterrag.registrate.util.entry.ItemEntry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.item.NavigatorItem;

public class ModItems {

    static {
	}

    public static final ItemEntry<NavigatorItem> NAVIGATOR = CreateRailwaysNavigator.REGISTRATE.item("navigator", NavigatorItem::new)
			.properties(p -> p.stacksTo(1))
            .tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.register();

    public static void init() {
    }
}
