package de.mrjulsen.crn.core.schedule;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;

public final class JourneyStop implements IJourneyEntry<JourneyStop> {

    private final int entryIndex;
    private final int orderIndex;

    private final String stationFilter;

    private JourneySection section;
    private String title;
    private volatile String stationName;
    private volatile boolean stationResolved;
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


    public int entryIndex() {
        return entryIndex;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getStationFilter() {
        return stationFilter;
    }

    public String getStationName() {
        return stationName;
    }

    public boolean isStationResolved() {
        return stationResolved;
    }

    public void resolveStationName(String stationName) {
        updateStationName(stationName);
        this.stationResolved = true;
    }

    public void unresolveStation() {
        this.stationResolved = false;
    }

    public void updateStationName(String stationName) {
        if (stationName != null && !stationName.isBlank()) {
            this.stationName = stationName;
        }
    }

    public String getTitle() {
        return title;
    }

    public JourneySection getSection() {
        return section;
    }

    public ScheduleEntry getScheduleEntry() {
        return entry;
    }


    @Override
    public String toString() {
        return "Stop[" + entryIndex + ", " + getStationName() + "]";
    }
}
