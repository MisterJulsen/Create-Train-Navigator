package de.mrjulsen.crn.navigator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * What a route search came back with: the routes worth offering, in the order the query asked for,
 * together with enough about the search itself to judge it.
 *
 * @param status         How the search turned out.
 * @param journeys       The routes found, best first according to the query's optimization.
 * @param computedAt     When the search ran, in transformed game ticks.
 * @param durationMs     How long it took, in milliseconds.
 * @param stationsSearched How many station nodes the search had available.
 * @param tripsScanned   How many travel opportunities it actually rode.
 */
public record NavigationResult(
    NavigationStatus status,
    List<RouteJourney> journeys,
    long computedAt,
    long durationMs,
    int stationsSearched,
    int tripsScanned
) {

    public NavigationResult {
        journeys = journeys == null ? List.of() : List.copyOf(journeys);
    }

    /** A result carrying nothing but the reason it is empty. */
    public static NavigationResult failed(NavigationStatus status, long computedAt, long durationMs) {
        return new NavigationResult(status, List.of(), computedAt, durationMs, 0, 0);
    }

    /** The route the query's optimization puts first. */
    public Optional<RouteJourney> best() {
        return journeys.isEmpty() ? Optional.empty() : Optional.of(journeys.get(0));
    }

    /** The route arriving earliest, whatever the query preferred. */
    public Optional<RouteJourney> fastest() {
        return journeys.stream().min(RouteOptimization.FASTEST.comparator());
    }

    /** The route with the fewest changes, whatever the query preferred. */
    public Optional<RouteJourney> mostComfortable() {
        return journeys.stream().min(RouteOptimization.FEWEST_TRANSFERS.comparator());
    }

    /** The routes ordered by when the traveller has to leave, as a departure board would list them. */
    public List<RouteJourney> byDeparture() {
        return journeys.stream().sorted(Comparator.comparingLong(RouteJourney::departure)).toList();
    }

    /** The routes needing no change of train. */
    public List<RouteJourney> directOnly() {
        return journeys.stream().filter(RouteJourney::isDirect).toList();
    }

    /** How many routes were found. */
    public int size() {
        return journeys.size();
    }

    /** Whether the search found nothing to offer. */
    public boolean isEmpty() {
        return journeys.isEmpty();
    }

    /** Whether the search found something. */
    public boolean isSuccess() {
        return status.isSuccess() && !journeys.isEmpty();
    }

    /** Serializes this result. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_STATUS, status.name());
        nbt.put(NBT_JOURNEYS, NbtHelper.writeList(journeys, RouteJourney::toNbt));
        nbt.putLong(NBT_COMPUTED_AT, computedAt);
        nbt.putLong(NBT_DURATION_MS, durationMs);
        nbt.putInt(NBT_STATIONS_SEARCHED, stationsSearched);
        nbt.putInt(NBT_TRIPS_SCANNED, tripsScanned);
        return nbt;
    }

    /** Deserializes a result written by {@link #toNbt()}. */
    public static NavigationResult fromNbt(CompoundTag nbt) {
        return new NavigationResult(
            NbtHelper.readEnum(nbt.getString(NBT_STATUS), NavigationStatus.class, NavigationStatus.NO_ROUTE),
            NbtHelper.readList(nbt, NBT_JOURNEYS, RouteJourney::fromNbt),
            nbt.getLong(NBT_COMPUTED_AT),
            nbt.getLong(NBT_DURATION_MS),
            nbt.getInt(NBT_STATIONS_SEARCHED),
            nbt.getInt(NBT_TRIPS_SCANNED)
        );
    }

    private static final String NBT_STATUS = "Status";
    private static final String NBT_JOURNEYS = "Journeys";
    private static final String NBT_COMPUTED_AT = "ComputedAt";
    private static final String NBT_DURATION_MS = "DurationMs";
    private static final String NBT_STATIONS_SEARCHED = "StationsSearched";
    private static final String NBT_TRIPS_SCANNED = "TripsScanned";
}
