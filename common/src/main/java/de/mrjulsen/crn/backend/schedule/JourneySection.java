package de.mrjulsen.crn.backend.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;

/**
 * One section of a train's journey, defined by a travel section instruction in the schedule, or a
 * single default section if the schedule defines none. A section groups consecutive stops and
 * assigns them a train line, a train category and usability information.
 */
public final class JourneySection implements IJourneyEntry<JourneySection> {

    private final int sectionIndex;
    private final int entryIndex;

    private final UUID trainCategoryId;
    private final UUID trainLineId;
    private final boolean includeNextSectionStart;
    private final boolean usable;
    private final boolean isDefault;

    private final List<JourneyStop> stops = new ArrayList<>();

    private JourneySection(int sectionIndex, int entryIndex, UUID trainCategoryId, UUID trainLineId, boolean includeNextSectionStart, boolean usable, boolean isDefault) {
        this.sectionIndex = sectionIndex;
        this.entryIndex = entryIndex;

        this.trainCategoryId = trainCategoryId;
        this.trainLineId = trainLineId;
        this.includeNextSectionStart = includeNextSectionStart;
        this.usable = usable;
        this.isDefault = isDefault;
    }

    JourneySection(int sectionIndex, int entryIndex, UUID trainCategoryId, UUID trainLineId, boolean includeNextSectionStart, boolean usable) {
        this(sectionIndex, entryIndex, trainCategoryId, trainLineId, includeNextSectionStart, usable, false);
    }

    static JourneySection def() {
        return new JourneySection(0, 0, null, null, true, true, true);
    }

    void addStop(JourneyStop stop) {
        stops.add(stop);
    }

    /** The position of this section within the journey (in travel order). */
    public int getSectionIndex() {
        return sectionIndex;
    }

    /** The schedule entry index of the instruction this section was created from. */
    public int entryIndex() {
        return entryIndex;
    }

    /** Whether this is the implicit section of a schedule without section instructions. */
    public boolean isDefault() {
        return isDefault;
    }

    /** Whether the following section's first stop still counts as part of this section. */
    public boolean includesNextSectionStart() {
        return includeNextSectionStart;
    }

    /** Whether this section may be used for navigation. */
    public boolean isUsable() {
        return usable;
    }

    /** The id of the assigned train category, or {@code null}. */
    public UUID getTrainCategoryId() {
        return trainCategoryId;
    }

    /** The id of the assigned train line, or {@code null}. */
    public UUID getTrainLineId() {
        return trainLineId;
    }

    /** The assigned train category, resolved from the global settings. Server-side only. */
    public Optional<TrainCategory> getTrainCategory() {
        return trainCategoryId == null ? Optional.empty() : GlobalSettings.getInstance().getTrainCategory(trainCategoryId);
    }

    /** The assigned train line, resolved from the global settings. Server-side only. */
    public Optional<TrainLine> getTrainLine() {
        return trainLineId == null ? Optional.empty() : GlobalSettings.getInstance().getTrainLine(trainLineId);
    }

    /** All stops of this section in travel order. */
    public List<JourneyStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    /** The first stop of this section, if it has any. */
    public Optional<JourneyStop> getFirstStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(0));
    }

    /** The last stop of this section, if it has any. */
    public Optional<JourneyStop> getLastStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(stops.size() - 1));
    }

    /** Whether the given stop is this section's first. */
    public boolean isFirstStop(JourneyStop stop) {
        return getFirstStop().map(x -> x == stop).orElse(false);
    }

    /** Whether the given stop is this section's last. */
    public boolean isLastStop(JourneyStop stop) {
        return getLastStop().map(x -> x == stop).orElse(false);
    }

    @Override
    public String toString() {
        return "Section[" + entryIndex + ", " + getFirstStop().map(JourneyStop::getStationName).orElse("unknown") + " -> " + getLastStop().map(JourneyStop::getStationName).orElse("unknown") + (usable ? "" : ", not in service") + "]";
    }
}
