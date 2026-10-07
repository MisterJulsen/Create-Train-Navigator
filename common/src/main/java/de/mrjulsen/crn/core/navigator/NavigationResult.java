package de.mrjulsen.crn.core.navigator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * The outcome of a route search: the journeys found, or the reason none were, together with a few
 * figures about the search itself. Journeys come ordered best first for the query's optimization.
 * <p>
 * Times are in game ticks on the backend's time base.
 *
 * @param status           Whether the search succeeded, and if not, why.
 * @param journeys         The journeys found, best first, or empty.
 * @param computedAt       When the search ran.
 * @param durationMs       How long the search took, in milliseconds.
 * @param stationsSearched How many stations the search covered.
 * @param tripsScanned     How many train trips the search examined.
 */
public record NavigationResult(
    NavigationStatus status,
    List<RouteJourney> journeys,
    long computedAt,
    long durationMs,
    int stationsSearched,
    int tripsScanned
) {

    private static final String NBT_STATUS = "Status";
    private static final String NBT_JOURNEYS = "Journeys";
    private static final String NBT_COMPUTED_AT = "ComputedAt";
    private static final String NBT_DURATION_MS = "DurationMs";
    private static final String NBT_STATIONS_SEARCHED = "StationsSearched";
    private static final String NBT_TRIPS_SCANNED = "TripsScanned";

    public NavigationResult {
        journeys = journeys == null ? List.of() : List.copyOf(journeys);
    }

    /** An empty result carrying the given failure reason. */
    public static NavigationResult failed(NavigationStatus status, long computedAt, long durationMs) {
        return new NavigationResult(status, List.of(), computedAt, durationMs, 0, 0);
    }

    /** The best journey found for the query's optimization, or empty. */
    public Optional<RouteJourney> best() {
        return journeys.isEmpty() ? Optional.empty() : Optional.of(journeys.get(0));
    }

    /** The journey that arrives earliest, or empty. */
    public Optional<RouteJourney> fastest() {
        return journeys.stream().min(RoutingStrategy.FASTEST.comparator());
    }

    /** The journey with the fewest transfers, or empty. */
    public Optional<RouteJourney> mostComfortable() {
        return journeys.stream().min(RoutingStrategy.FEWEST_TRANSFERS.comparator());
    }

    /** The found journeys ordered by departure time. */
    public List<RouteJourney> byDeparture() {
        return journeys.stream().sorted(Comparator.comparingLong(RouteJourney::departure)).toList();
    }

    /** Only the found journeys that need no transfer. */
    public List<RouteJourney> directOnly() {
        return journeys.stream().filter(RouteJourney::isDirect).toList();
    }

    /** How many journeys were found. */
    public int size() {
        return journeys.size();
    }

    /** Whether no journey was found. */
    public boolean isEmpty() {
        return journeys.isEmpty();
    }

    /** Whether the search succeeded and found at least one journey. */
    public boolean isSuccess() {
        return status.isSuccess() && !journeys.isEmpty();
    }

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
}
