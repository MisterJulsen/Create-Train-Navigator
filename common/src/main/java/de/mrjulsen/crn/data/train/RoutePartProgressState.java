package de.mrjulsen.crn.data.train;

import java.util.Arrays;

public enum RoutePartProgressState {
    BEFORE(0),
    AT_START(1),
    TRAVELING(2),
    NEXT_STOP_ANNOUNCED(3),
    AT_STOPOVER(4),
    END_ANNOUNCED(4),
    AT_END(5),
    AFTER(6);

    private int index;

    private RoutePartProgressState(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static RoutePartProgressState getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(BEFORE);
    }

    public boolean isAnyStopAnnounced() {
        return this == NEXT_STOP_ANNOUNCED || this == END_ANNOUNCED;
    }

    public boolean isAtAnyStop() {
        return this == AT_START || this == AT_STOPOVER || this == AT_END;
    }

    public boolean isOutOfBounds() {
        return this == BEFORE || this == AFTER;
    }
}
