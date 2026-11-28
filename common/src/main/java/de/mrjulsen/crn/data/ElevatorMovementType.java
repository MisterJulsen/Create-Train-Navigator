/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
