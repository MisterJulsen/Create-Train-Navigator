package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * An immutable snapshot of a train category: the configured category plus which trains and lines
 * currently carry it.
 *
 * @param category      The train category itself.
 * @param trainIds      The trains currently operating under this category.
 * @param lines         The train lines currently operating under this category.
 * @param delayedTrains How many of its trains are currently late.
 */
public record CategorySnapshot(
    TrainCategory category,
    Set<UUID> trainIds,
    List<TrainLine> lines,
    int delayedTrains
) {

    public CategorySnapshot {
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** The category's id. */
    public UUID id() {
        return category.getId();
    }

    /** The category's name. */
    public String name() {
        return category.getCategoryName();
    }

    /** How many trains currently operate under this category. */
    public int trainCount() {
        return trainIds.size();
    }

    /** Whether any train currently operates under this category. */
    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    /** Whether any train of this category is currently late. */
    public boolean hasDelays() {
        return delayedTrains > 0;
    }
}
