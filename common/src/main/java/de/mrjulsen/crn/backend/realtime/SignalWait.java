package de.mrjulsen.crn.backend.realtime;

import java.util.Set;
import java.util.UUID;

/**
 * One completed or ongoing wait at a signal during the current leg.
 *
 * @param signalId          The id of the signal boundary the train waited at.
 * @param waitTicks         How long the train waited at this signal.
 * @param occupyingTrains   The names of the trains that occupied the signal block when the wait started.
 * @param occupyingTrainIds The ids of those trains, so their live state (e.g. their own delay) can be queried.
 */
public record SignalWait(UUID signalId, int waitTicks, Set<String> occupyingTrains, Set<UUID> occupyingTrainIds) {}
