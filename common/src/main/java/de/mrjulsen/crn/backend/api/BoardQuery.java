package de.mrjulsen.crn.backend.api;

import java.util.UUID;
import java.util.function.Predicate;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * What a departure or arrival board should contain.
 * <p>
 * Built by starting from {@link #defaults()} and refining it, e.g.
 * {@code BoardQuery.defaults().withLimit(8).onlyLine(lineId)}. Every option narrows the result, so
 * they can be combined freely.
 *
 * @param limit             The maximum number of entries.
 * @param fromTime          Only entries departing at or after this transformed game time.
 *                          {@code 0} or less includes all.
 * @param withinTicks       Only entries departing within this many ticks from {@link #fromTime()},
 *                          or {@code 0} or less for no upper bound.
 * @param includeUnreliable Whether to include trains whose times are still being learned.
 * @param includeCancelled  Whether to include trains that are out of service.
 * @param deduplicateTrains Whether to include at most one entry per train.
 * @param lineId            Only entries of this train line, or {@code null} for all.
 * @param categoryId        Only entries of this train category, or {@code null} for all.
 * @param destination       Only entries advertising this destination, or {@code null} for all.
 * @param filter            An additional test every entry must pass, or {@code null} for none.
 */
public record BoardQuery(
    int limit,
    long fromTime,
    long withinTicks,
    boolean includeUnreliable,
    boolean includeCancelled,
    boolean deduplicateTrains,
    UUID lineId,
    UUID categoryId,
    String destination,
    Predicate<BoardEntry> filter
) {

    /** Unlimited, excluding unreliable and cancelled trains, at most one entry per train. */
    public static BoardQuery defaults() {
        return new BoardQuery(Integer.MAX_VALUE, 0, 0, false, false, true, null, null, null, null);
    }

    /** Everything the backend knows, including unreliable and cancelled trains. */
    public static BoardQuery all() {
        return new BoardQuery(Integer.MAX_VALUE, 0, 0, true, true, false, null, null, null, null);
    }

    /** A copy with at most this many entries. */
    public BoardQuery withLimit(int limit) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy including only entries departing at or after the given transformed game time. */
    public BoardQuery from(long fromTime) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy including only entries departing within the given number of ticks. */
    public BoardQuery within(long withinTicks) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy that also includes trains whose times are still being learned. */
    public BoardQuery withUnreliable() {
        return new BoardQuery(limit, fromTime, withinTicks, true, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy that also includes trains which are out of service. */
    public BoardQuery withCancelled() {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, true, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy that may show a train more than once, e.g. for a station it calls at twice. */
    public BoardQuery withDuplicates() {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, false, lineId, categoryId, destination, filter);
    }

    /** A copy restricted to one train line. */
    public BoardQuery onlyLine(UUID lineId) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy restricted to one train line. */
    public BoardQuery onlyLine(TrainLine line) {
        return onlyLine(line == null ? null : line.getId());
    }

    /** A copy restricted to one train category. */
    public BoardQuery onlyCategory(UUID categoryId) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy restricted to one train category. */
    public BoardQuery onlyCategory(TrainCategory category) {
        return onlyCategory(category == null ? null : category.getId());
    }

    /** A copy restricted to trains advertising the given destination. */
    public BoardQuery onlyDestination(String destination) {
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, filter);
    }

    /** A copy restricted to trains terminating at the given station. */
    public BoardQuery onlyDestination(StationRef destination) {
        return onlyDestination(destination == null ? null : destination.name());
    }

    /** A copy that additionally applies the given test to every entry. */
    public BoardQuery matching(Predicate<BoardEntry> filter) {
        Predicate<BoardEntry> combined = this.filter == null ? filter : this.filter.and(filter);
        return new BoardQuery(limit, fromTime, withinTicks, includeUnreliable, includeCancelled, deduplicateTrains, lineId, categoryId, destination, combined);
    }

    /** Whether the given entry satisfies every restriction of this query. */
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

    /** Whether a departure at the given time falls inside this query's time window. */
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
