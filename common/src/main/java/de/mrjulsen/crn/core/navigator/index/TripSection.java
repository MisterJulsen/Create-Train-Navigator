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
