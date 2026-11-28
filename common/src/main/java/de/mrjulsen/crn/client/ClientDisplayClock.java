/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client;

import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class ClientDisplayClock {

    private static final long RESYNC_THRESHOLD = 200;

    private static long offset;
    private static boolean initialized;

    private ClientDisplayClock() {}

    public static long now() {
        long actual = ModUtils.getTransformedWorldTime();
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            initialized = false;
            return actual;
        }

        long currentOffset = actual - level.getGameTime();
        if (!initialized || Math.abs(currentOffset - offset) >= RESYNC_THRESHOLD) {
            offset = currentOffset;
            initialized = true;
        }
        return level.getGameTime() + offset;
    }
}
