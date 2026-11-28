/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.data;

import net.minecraft.core.Direction;

public record CarriageData(int index, Direction assemblyDirection, boolean isOppositeDirection) {}
