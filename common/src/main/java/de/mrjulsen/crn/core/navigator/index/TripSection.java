/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.navigator.index;

import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.ref.StationRef;

public record TripSection(int sectionIndex, LineRef line, TrainCategoryRef category, StationRef destination, boolean includesNextSectionStart) {

    public TripSection {
        line = line == null ? LineRef.NONE : line;
        category = category == null ? TrainCategoryRef.NONE : category;
        destination = destination == null ? StationRef.NONE : destination;
    }
}
