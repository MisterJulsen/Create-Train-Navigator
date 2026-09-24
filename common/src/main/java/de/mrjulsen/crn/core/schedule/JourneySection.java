package de.mrjulsen.crn.core.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.crn.data.settings.GlobalSettings;

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

    public int getSectionIndex() {
        return sectionIndex;
    }

    public int entryIndex() {
        return entryIndex;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean includesNextSectionStart() {
        return includeNextSectionStart;
    }

    public boolean isUsable() {
        return usable;
    }

    public UUID getTrainCategoryId() {
        return trainCategoryId;
    }

    public UUID getTrainLineId() {
        return trainLineId;
    }

    public Optional<TrainCategory> getTrainCategory() {
        return trainCategoryId == null ? Optional.empty() : GlobalSettings.getInstance().getTrainCategory(trainCategoryId);
    }

    public Optional<TrainLine> getTrainLine() {
        return trainLineId == null ? Optional.empty() : GlobalSettings.getInstance().getTrainLine(trainLineId);
    }

    public List<JourneyStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public Optional<JourneyStop> getFirstStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(0));
    }

    public Optional<JourneyStop> getLastStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(stops.size() - 1));
    }

    public boolean isFirstStop(JourneyStop stop) {
        return getFirstStop().map(x -> x == stop).orElse(false);
    }

    public boolean isLastStop(JourneyStop stop) {
        return getLastStop().map(x -> x == stop).orElse(false);
    }

    @Override
    public String toString() {
        return "Section[" + entryIndex + ", " + getFirstStop().map(JourneyStop::getStationName).orElse("unknown") + " -> " + getLastStop().map(JourneyStop::getStationName).orElse("unknown") + (usable ? "" : ", not in service") + "]";
    }
}
