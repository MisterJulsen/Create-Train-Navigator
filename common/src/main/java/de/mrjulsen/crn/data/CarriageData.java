/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.data;

import net.minecraft.core.Direction;

public record CarriageData(int index, Direction assemblyDirection, boolean isOppositeDirection) {}
