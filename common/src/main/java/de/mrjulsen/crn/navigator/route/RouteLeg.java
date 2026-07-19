package de.mrjulsen.crn.navigator.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * One ride on one service: the traveller gets on at the first call and off at the last, staying
 * seated for everything in between.
 * <p>
 * A leg never spans a change of train and never spans a change of service, so the line, the category
 * and the advertised destination hold for the whole of it.
 *
 * @param trainId         The id of the train operating this leg.
 * @param sessionId       The train's tracking session at the time of the search. A change of it means
 *                        the route was planned against data that no longer applies.
 * @param trainName       The train's own name.
 * @param displayName     The line name if one is assigned, otherwise the train name.
 * @param line            The train line this leg runs on, or {@code null} if it carries none.
 * @param category        The train category this leg carries, or {@code null} if it carries none.
 * @param destinationText What the train advertises as its destination, for the departure board.
 * @param sectionIndex    The position of the section this leg is operated as in the train's journey.
 * @param calls           Every station this leg calls at, boarding first and alighting last.
 */
public record RouteLeg(
    UUID trainId,
    UUID sessionId,
    String trainName,
    String displayName,
    TrainLine line,
    TrainCategory category,
    String destinationText,
    int sectionIndex,
    List<RouteCall> calls
) {

    public RouteLeg {
        calls = calls == null ? List.of() : List.copyOf(calls);
    }

    /** Where the traveller gets on. */
    public RouteCall boarding() {
        return calls.get(0);
    }

    /** Where the traveller gets off. */
    public RouteCall alighting() {
        return calls.get(calls.size() - 1);
    }

    /** The station the traveller gets on at. */
    public StationRef from() {
        return boarding().station();
    }

    /** The station the traveller gets off at. */
    public StationRef to() {
        return alighting().station();
    }

    /** When the train leaves the boarding station, in transformed game ticks. */
    public long departure() {
        return boarding().departure();
    }

    /** When the train reaches the alighting station, in transformed game ticks. */
    public long arrival() {
        return alighting().arrival();
    }

    /** How long the traveller spends on this train, in ticks. */
    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    /** The stations passed between getting on and getting off, in travel order. */
    public List<RouteCall> intermediateCalls() {
        return calls.size() < 3 ? List.of() : calls.subList(1, calls.size() - 1);
    }

    /** How many stations lie between getting on and getting off. */
    public int intermediateStopCount() {
        return Math.max(0, calls.size() - 2);
    }

    /** The train line this leg runs on, if it carries one. */
    public Optional<TrainLine> trainLine() {
        return Optional.ofNullable(line);
    }

    /** The train category this leg carries, if any. */
    public Optional<TrainCategory> trainCategory() {
        return Optional.ofNullable(category);
    }

    @Override
    public String toString() {
        return displayName + " " + from().name() + " " + departure() + " -> " + to().name() + " " + arrival();
    }
}
