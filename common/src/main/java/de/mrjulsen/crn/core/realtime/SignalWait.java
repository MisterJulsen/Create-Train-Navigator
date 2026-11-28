/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.realtime;

import java.util.Set;
import java.util.UUID;

public record SignalWait(UUID signalId, int waitTicks, Set<String> occupyingTrains, Set<UUID> occupyingTrainIds) {}
