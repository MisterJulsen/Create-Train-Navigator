package de.mrjulsen.crn.navigator.index;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * The service a stretch of a trip is operated as: which line and category it carries and what it
 * advertises as its destination.
 * <p>
 * Shared by every call belonging to it, and across all cycles of the same trip, so a train that
 * repeats a hundred times still holds one of these per section.
 *
 * @param sectionIndex The position of the section in the train's journey.
 * @param line         The train line, or {@code null} if the section carries none.
 * @param category     The train category, or {@code null} if the section carries none.
 * @param destination  The terminus advertised for this section.
 * @param includesNextSectionStart Whether this section still advertises the following one's first
 *                     stop. This is what decides whether a traveller may stay seated across the
 *                     boundary: without it the train is doing something else beyond this section
 *                     and carries no passengers over it.
 */
public record TripSection(int sectionIndex, TrainLine line, TrainCategory category, StationRef destination, boolean includesNextSectionStart) {

    public TripSection {
        destination = destination == null ? StationRef.NONE : destination;
    }
}
