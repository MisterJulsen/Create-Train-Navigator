package de.mrjulsen.crn.core.realtime;

import java.util.Set;
import java.util.UUID;

public record SignalWait(UUID signalId, int waitTicks, Set<String> occupyingTrains, Set<UUID> occupyingTrainIds) {}
