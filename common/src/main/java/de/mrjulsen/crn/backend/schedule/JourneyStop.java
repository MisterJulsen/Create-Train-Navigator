package de.mrjulsen.crn.backend.schedule;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;

/**
 * One stop of a train's journey, created from a destination instruction in the schedule.
 * <p>
 * The schedule information (filter, title, section) is immutable. Only the resolved station name is
 * updated live, since a filter containing wildcards can resolve to different stations over time.
 */
public final class JourneyStop implements IJourneyEntry<JourneyStop> {

    private final int entryIndex;
    private final int orderIndex;

    private final String stationFilter;

    private JourneySection section;
    private String title;
    private volatile String stationName;
    private final transient ScheduleEntry entry;


    JourneyStop(int entryIndex, int orderIndex, String stationFilter, ScheduleEntry entry) {
        this.entryIndex = entryIndex;
        this.orderIndex = orderIndex;
        this.stationFilter = stationFilter;
        this.entry = entry;
        this.stationName = stationFilter;
    }

    void setSection(JourneySection section) {
        this.section = section;
    }

    void setTitle(String title) {
        this.title = title;
    }


    /** The index of the destination instruction in the schedule. */
    public int entryIndex() {
        return entryIndex;
    }

    /** The position of this stop in travel order (0 = first destination entry in the schedule). */
    public int getOrderIndex() {
        return orderIndex;
    }

    /** The raw station filter of this stop. May contain wildcards. */
    public String getStationFilter() {
        return stationFilter;
    }

    /** The station this stop currently resolves to. Falls back to the filter text. */
    public String getStationName() {
        return stationName;
    }

    /** Updates the station this stop resolves to. Blank names are ignored. */
    public void updateStationName(String stationName) {
        if (stationName != null && !stationName.isBlank()) {
            this.stationName = stationName;
        }
    }

    /** The schedule title the train carries while traveling towards this stop. */
    public String getTitle() {
        return title;
    }

    /** The section this stop belongs to. */
    public JourneySection getSection() {
        return section;
    }

    /** The underlying schedule entry, used to evaluate this stop's wait conditions. */
    public ScheduleEntry getScheduleEntry() {
        return entry;
    }


    @Override
    public String toString() {
        return "Stop[" + entryIndex + ", " + getStationName() + "]";
    }
}
