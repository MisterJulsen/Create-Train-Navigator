/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class ModPartials {

    public static final PartialModel TRACK_PENALTY_ANCHOR_OVERLAY = block("track_overlay/penalty_anchor");

    private static PartialModel block(String path) {
        return PartialModel.of(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "block/" + path));
    }

    public static void init() {}
}
