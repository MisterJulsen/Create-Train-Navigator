package de.mrjulsen.crn.data;

public enum ElevatorMovementType {
    STANDING_STILL("", ""),
    GOING_UP("↑", "▲"),
    GOING_DOWN("↓", "▼");

    final String arrow;
    final String triangle;

    ElevatorMovementType(String arrow, String triangle) {
        this.arrow = arrow;
        this.triangle = triangle;
    }

    public String getArrow() {
        return arrow;
    }

    public String getTriangle() {
        return triangle;
    }
}
