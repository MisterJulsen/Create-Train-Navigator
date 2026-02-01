package de.mrjulsen.crn.data.train;

import java.util.Arrays;

public enum ETrainStopState {
    ARRIVAL(0),
    DEPARTURE(1);

    private final int id;

    private ETrainStopState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ETrainStopState getById(int id) {
        return Arrays.stream(values()).filter(e -> e.id == id).findFirst().orElse(null);
    }

    public static ETrainStopState beforeArrival(boolean beforeArrival) {
        return beforeArrival ? ARRIVAL : DEPARTURE;
    }

    public ScheduleSection resolveSection(ScheduleSection section, int waitingAtStationIdx, int currentScheduleIdx) {
        if (section == null) {
            return null;
        }

        ScheduleSection prevSection = section.previousSection();
        boolean isFirstStationInSection = section.getFirstStop().map(x -> x.getEntryIndex() == currentScheduleIdx).orElse(false);
        return switch (this) {
            case ARRIVAL -> {
                if (isFirstStationInSection && (prevSection.shouldIncludeNextStationOfNextSection() || !section.isUsable())) {
                    yield prevSection;
                }
                yield section;
            }
            default -> {
                if (!section.isUsable()) {
                    yield prevSection;
                }
                yield section;
            }
        };
    }
}
