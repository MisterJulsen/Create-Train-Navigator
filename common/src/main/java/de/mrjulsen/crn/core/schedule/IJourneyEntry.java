package de.mrjulsen.crn.core.schedule;

import org.jetbrains.annotations.NotNull;

public interface IJourneyEntry<T extends IJourneyEntry<T>> extends Comparable<T> {

    int entryIndex();

    @Override
    default int compareTo(@NotNull T o) {
        return Integer.compare(entryIndex(), o.entryIndex());
    }
}
