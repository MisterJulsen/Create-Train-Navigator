package de.mrjulsen.crn.backend.schedule;

import org.jetbrains.annotations.NotNull;

/** An element of a journey that is anchored to a schedule entry and ordered by it. */
public interface IJourneyEntry<T extends IJourneyEntry<T>> extends Comparable<T> {

    /** The index of the schedule entry this element was created from. */
    int entryIndex();

    @Override
    default int compareTo(@NotNull T o) {
        return Integer.compare(entryIndex(), o.entryIndex());
    }
}
