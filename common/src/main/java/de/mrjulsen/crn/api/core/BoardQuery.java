package de.mrjulsen.crn.api.core;

import java.util.UUID;
import java.util.function.Predicate;

import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.data.settings.TrainLine;

/**
 * What a departure or arrival board should contain. Instances are immutable; every method that
 * narrows the query returns a new one, so a query can be built up by chaining and a prepared query
 * can be reused for several stations.
 * <p>
 * Start from {@link #defaults()} for a board fit to show to players, or {@link #all()} to see
 * everything the backend holds. All times are in the unit described by
 * {@link RailwayBackendApi#currentTime()}.
 *
 * @param limit             The largest number of entries to return.
 * @param fromTime          The earliest departure to include. Zero or less means no lower bound.
 * @param withinTicks       How far past {@code fromTime} to look. Zero or less means no upper bound.
 * @param includeUnreliable Whether to include trains whose data the backend does not yet consider
 *                          dependable, such as those that have not completed a run.
 * @param includeCancelled  Whether to include trains that are out of service.
 * @param deduplicateTrains Whether to keep only the earliest entry per train, so a train calling
 *                          more than once appears once.
 * @param includeDivertedAway Whether to keep a train that was timetabled at this station but is
 *                          being diverted to another one, so the board can announce where it goes
 *                          instead. Such an entry reports the station it really calls at, which is
 *                          not the one asked for; see {@link BoardEntry#isDiverted()}.
 * @param lineId            If set, only entries of this line.
 * @param categoryId        If set, only entries of this category.
 * @param destination       If set, only entries whose destination text matches, ignoring case.
 * @param filter            An additional test every entry must pass.
 */
public record BoardQuery(
    int limit,
    long fromTime,
    long withinTicks,
    boolean includeUnreliable,
    boolean includeCancelled,
    boolean deduplicateTrains,
    boolean includeDivertedAway,
    UUID lineId,
    UUID categoryId,
    String destination,
    Predicate<BoardEntry> filter
) {

    /**
     * A board as it would be shown publicly: unlimited in size and time, without unreliable or
     * out-of-service trains, with each train appearing only once, and with trains diverted away
     * from the station still listed.
     */
    public static BoardQuery defaults() {
        return new BoardQuery(Integer.MAX_VALUE, 0, 0, false, false, true, true, null, null, null, null);
    }

    /**
     * Everything the backend has, including unreliable and out-of-service trains and repeated calls
     * of the same train.
     */
    public static BoardQuery all() {
        return new BoardQuery(Integer.MAX_VALUE, 0, 0, true, true, false, true, null, null, null, null);
    }

    /** Returns at most this many entries. */
    public BoardQuery withLimit(int limit) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Starts the board at the given time instead of now. */
    public BoardQuery from(long fromTime) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Ends the board this many ticks after its start time. */
    public BoardQuery within(long withinTicks) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Also includes trains whose data is not yet dependable. */
    public BoardQuery withUnreliable() {
        return new BoardQuery(limit, fromTime, withinTicks, true, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Also includes trains that are out of service. */
    public BoardQuery withCancelled() {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, true, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Keeps every call of a train rather than only its earliest. */
    public BoardQuery withDuplicates() {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, false, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /**
     * Keeps only the trains really calling at the station, dropping those merely timetabled there
     * and diverted elsewhere.
     */
    public BoardQuery withoutDivertedAway() {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, false, lineId, categoryId, destination, filter);
    }

    /** Restricts the board to one line. Passing {@code null} lifts the restriction. */
    public BoardQuery onlyLine(UUID lineId) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Restricts the board to one line. Passing {@code null} lifts the restriction. */
    public BoardQuery onlyLine(TrainLine line) {
        return onlyLine(line == null ? null : line.getId());
    }

    /** Restricts the board to one category. Passing {@code null} lifts the restriction. */
    public BoardQuery onlyCategory(UUID categoryId) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** Restricts the board to one category. Passing {@code null} lifts the restriction. */
    public BoardQuery onlyCategory(TrainCategory category) {
        return onlyCategory(category == null ? null : category.getId());
    }

    /**
     * Restricts the board to entries bound for this destination. Compared against the entry's
     * displayed destination text, ignoring case.
     */
    public BoardQuery onlyDestination(String destination) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, filter);
    }

    /** As above, using the station's display name. */
    public BoardQuery onlyDestination(StationRef destination) {
        return onlyDestination(destination == null ? null : destination.displayName());
    }

    /** Adds a test an entry must pass. Repeated calls combine, so every added test must hold. */
    public BoardQuery matching(Predicate<BoardEntry> filter) {
        Predicate<BoardEntry> combined = this.filter == null ? filter : this.filter.and(filter);
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, includeDivertedAway, lineId, categoryId, destination, combined);
    }

    /**
     * Whether an entry passes the line, category and destination restrictions and the added tests.
     * The time window is checked separately by {@link #acceptsTime(long)}.
     */
    public boolean accepts(BoardEntry entry) {
        if (lineId != null && !lineId.equals(entry.line().id())) {
            return false;
        }
        if (categoryId != null && !categoryId.equals(entry.category().id())) {
            return false;
        }
        if (destination != null && !destination.equalsIgnoreCase(entry.destinationText())) {
            return false;
        }
        return filter == null || filter.test(entry);
    }

    /** Whether a departure time falls inside the query's window. Unknown times never do. */
    public boolean acceptsTime(long departure) {
        if (departure < 0) {
            return false;
        }
        if (fromTime > 0 && departure < fromTime) {
            return false;
        }
        return withinTicks <= 0 || departure <= Math.max(fromTime, 0) + withinTicks;
    }
}
