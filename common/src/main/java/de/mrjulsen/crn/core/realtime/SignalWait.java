/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.realtime;

import java.util.Set;
import java.util.UUID;

public record SignalWait(UUID signalId, int waitTicks, Set<String> occupyingTrains, Set<UUID> occupyingTrainIds) {}
