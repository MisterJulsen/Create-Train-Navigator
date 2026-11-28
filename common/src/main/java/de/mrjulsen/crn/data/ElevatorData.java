/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.data;

public record ElevatorData(String currentShortName, String currentLongName, String destinationShortName, String destinationLongName, ElevatorMovementType sign) {}
