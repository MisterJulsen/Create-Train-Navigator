/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.schedule;

import org.jetbrains.annotations.NotNull;

public interface IJourneyEntry<T extends IJourneyEntry<T>> extends Comparable<T> {

    int entryIndex();

    @Override
    default int compareTo(@NotNull T o) {
        return Integer.compare(entryIndex(), o.entryIndex());
    }
}
