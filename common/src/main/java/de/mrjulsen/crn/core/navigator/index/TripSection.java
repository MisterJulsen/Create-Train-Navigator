package de.mrjulsen.crn.core.navigator.index;

import de.mrjulsen.crn.api.core.CategoryRef;
import de.mrjulsen.crn.api.core.LineRef;
import de.mrjulsen.crn.api.core.StationRef;

public record TripSection(int sectionIndex, LineRef line, CategoryRef category, StationRef destination, boolean includesNextSectionStart) {

    public TripSection {
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        destination = destination == null ? StationRef.NONE : destination;
    }
}
