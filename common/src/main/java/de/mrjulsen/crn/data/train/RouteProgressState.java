package de.mrjulsen.crn.data.train;

import java.util.Arrays;

public enum RouteProgressState {
    BEFORE(0),
    AT_START(1),
    TRAVELING(2),
    NEXT_STOP_ANNOUNCED(3),
    AT_STOPOVER(4),
    TRANSFER_ANNOUNCED(5),
    AT_TRANSFER(6),
    WHILE_TRANSFER(7),
    BEFORE_CONTINUATION(8),
    END_ANNOUNCED(9),
    AT_END(10),
    AFTER(11);

    private int index;

    private RouteProgressState(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static RouteProgressState getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(BEFORE);
    }

    public boolean isAnyStopAnnounced() {
        return this == NEXT_STOP_ANNOUNCED || this == END_ANNOUNCED || this == TRANSFER_ANNOUNCED;
    }

    public boolean isAtAnyStop() {
        return this == AT_START || this == AT_STOPOVER || this == AT_END || this == AT_TRANSFER || this == BEFORE_CONTINUATION;
    }

    public boolean isOutOfBounds() {
        return this == BEFORE || this == AFTER;
    }

    public boolean isTransferring() {
        return this == AT_TRANSFER || this == WHILE_TRANSFER || this == BEFORE_CONTINUATION;
    }

    public boolean isWaiting() {
        return isOutOfBounds() || this == WHILE_TRANSFER;
    }
}
