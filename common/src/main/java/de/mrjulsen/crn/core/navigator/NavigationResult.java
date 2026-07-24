package de.mrjulsen.crn.core.navigator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

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

    public static NavigationResult failed(NavigationStatus status, long computedAt, long durationMs) {
        return new NavigationResult(status, List.of(), computedAt, durationMs, 0, 0);
    }

    public Optional<RouteJourney> best() {
        return journeys.isEmpty() ? Optional.empty() : Optional.of(journeys.get(0));
    }

    public Optional<RouteJourney> fastest() {
        return journeys.stream().min(RouteOptimization.FASTEST.comparator());
    }

    public Optional<RouteJourney> mostComfortable() {
        return journeys.stream().min(RouteOptimization.FEWEST_TRANSFERS.comparator());
    }

    public List<RouteJourney> byDeparture() {
        return journeys.stream().sorted(Comparator.comparingLong(RouteJourney::departure)).toList();
    }

    public List<RouteJourney> directOnly() {
        return journeys.stream().filter(RouteJourney::isDirect).toList();
    }

    public int size() {
        return journeys.size();
    }

    public boolean isEmpty() {
        return journeys.isEmpty();
    }

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

    private static final String NBT_STATUS = "Status";
    private static final String NBT_JOURNEYS = "Journeys";
    private static final String NBT_COMPUTED_AT = "ComputedAt";
    private static final String NBT_DURATION_MS = "DurationMs";
    private static final String NBT_STATIONS_SEARCHED = "StationsSearched";
    private static final String NBT_TRIPS_SCANNED = "TripsScanned";
}
