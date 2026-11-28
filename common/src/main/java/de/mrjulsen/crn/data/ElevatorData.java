/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.data;

public record ElevatorData(String currentShortName, String currentLongName, String destinationShortName, String destinationLongName, ElevatorMovementType sign) {}
