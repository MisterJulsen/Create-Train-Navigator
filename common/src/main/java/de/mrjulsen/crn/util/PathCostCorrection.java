/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.util;

import java.util.function.Supplier;
import de.mrjulsen.crn.config.ModServerConfig;

public final class PathCostCorrection {

    private static final ThreadLocal<Integer> SCOPE = ThreadLocal.withInitial(() -> 0);

    private PathCostCorrection() {}

    public static boolean isActive() {
        return SCOPE.get() > 0 || isEnabledGlobally();
    }

    public static <T> T run(Supplier<T> action) {
        SCOPE.set(SCOPE.get() + 1);
        try {
            return action.get();
        } finally {
            SCOPE.set(SCOPE.get() - 1);
        }
    }

    private static boolean isEnabledGlobally() {
        return ModServerConfig.SPEC.isLoaded() && ModServerConfig.FIX_CREATE_PATH_COST.get();
    }
}
