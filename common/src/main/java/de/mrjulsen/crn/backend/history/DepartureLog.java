package de.mrjulsen.crn.backend.history;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import de.mrjulsen.crn.data.train.TrainUtils;
import net.minecraft.nbt.CompoundTag;

/**
 * Remembers, per station, when a train last departed - overall and split by the departing train's
 * line, category and name.
 * <p>
 * Only the latest time per key is kept, and it is kept for good: a line that left a station once is
 * remembered as having left it however long ago, no matter how many other trains have departed
 * since. That is what {@link de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition} needs
 * to space trains apart, and what the station's goggle tooltip shows.
 *
 * <h2>Threading</h2>
 * Departures are recorded on the server thread and read from packet handlers on others, so the maps
 * are concurrent and the per-station record is published as a whole.
 */
public final class DepartureLog {

    /** No departure of the requested kind has ever been recorded. */
    public static final long NEVER = Long.MIN_VALUE;

    /** The last departure times at one station, by kind. */
    private static final class Departures {

        private static final String NBT_LAST = "Last";
        private static final String NBT_LINES = "Lines";
        private static final String NBT_CATEGORIES = "Categories";
        private static final String NBT_NAMES = "Names";

        private volatile long last = NEVER;
        private final Map<UUID, Long> byLine = new ConcurrentHashMap<>();
        private final Map<UUID, Long> byCategory = new ConcurrentHashMap<>();
        private final Map<String, Long> byName = new ConcurrentHashMap<>();

        void record(long time, UUID lineId, UUID categoryId, String trainName) {
            last = Math.max(last, time);
            if (lineId != null) {
                byLine.merge(lineId, time, Math::max);
            }
            if (categoryId != null) {
                byCategory.merge(categoryId, time, Math::max);
            }
            if (trainName != null && !trainName.isBlank()) {
                byName.merge(trainName, time, Math::max);
            }
        }

        CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong(NBT_LAST, last);
            nbt.put(NBT_LINES, writeUuidMap(byLine));
            nbt.put(NBT_CATEGORIES, writeUuidMap(byCategory));
            nbt.put(NBT_NAMES, writeNameMap(byName));
            return nbt;
        }

        static Departures fromNbt(CompoundTag nbt) {
            Departures departures = new Departures();
            departures.last = nbt.getLong(NBT_LAST);
            readUuidMap(nbt.getCompound(NBT_LINES), departures.byLine);
            readUuidMap(nbt.getCompound(NBT_CATEGORIES), departures.byCategory);
            CompoundTag names = nbt.getCompound(NBT_NAMES);
            for (String key : names.getAllKeys()) {
                departures.byName.put(key, names.getLong(key));
            }
            return departures;
        }

        private static CompoundTag writeUuidMap(Map<UUID, Long> map) {
            CompoundTag nbt = new CompoundTag();
            map.forEach((key, value) -> nbt.putLong(key.toString(), value));
            return nbt;
        }

        private static CompoundTag writeNameMap(Map<String, Long> map) {
            CompoundTag nbt = new CompoundTag();
            map.forEach(nbt::putLong);
            return nbt;
        }

        private static void readUuidMap(CompoundTag nbt, Map<UUID, Long> target) {
            for (String key : nbt.getAllKeys()) {
                try {
                    target.put(UUID.fromString(key), nbt.getLong(key));
                } catch (IllegalArgumentException ignored) {
                    // A malformed id is dropped rather than failing the whole load.
                }
            }
        }
    }

    private final Map<String, Departures> departuresByStation = new ConcurrentHashMap<>();

    /** Records a departure at the given station under the departing train's line, category and name. */
    public void record(String stationName, long time, UUID lineId, UUID categoryId, String trainName) {
        if (stationName == null || stationName.isBlank()) {
            return;
        }
        departuresByStation.computeIfAbsent(stationName, x -> new Departures()).record(time, lineId, categoryId, trainName);
    }

    /** The most recent departure at any station matching the filter, or {@link #NEVER}. */
    public long getLastDeparture(String stationFilter) {
        long latest = NEVER;
        for (Map.Entry<String, Departures> e : departuresByStation.entrySet()) {
            if (TrainUtils.stationMatches(e.getKey(), stationFilter)) {
                latest = Math.max(latest, e.getValue().last);
            }
        }
        return latest;
    }

    /** The most recent departure of the given line at any station matching the filter. */
    public long getLastDepartureOfLine(String stationFilter, UUID lineId) {
        return lineId == null ? NEVER : maxOverStations(stationFilter, d -> d.byLine.getOrDefault(lineId, NEVER));
    }

    /** The most recent departure of the given category at any station matching the filter. */
    public long getLastDepartureOfCategory(String stationFilter, UUID categoryId) {
        return categoryId == null ? NEVER : maxOverStations(stationFilter, d -> d.byCategory.getOrDefault(categoryId, NEVER));
    }

    /** The most recent departure of the named train at any station matching the filter. */
    public long getLastDepartureOfName(String stationFilter, String trainName) {
        return trainName == null ? NEVER : maxOverStations(stationFilter, d -> d.byName.getOrDefault(trainName, NEVER));
    }

    private long maxOverStations(String stationFilter, ToLongFunction<Departures> pick) {
        long latest = NEVER;
        for (Map.Entry<String, Departures> e : departuresByStation.entrySet()) {
            if (TrainUtils.stationMatches(e.getKey(), stationFilter)) {
                latest = Math.max(latest, pick.applyAsLong(e.getValue()));
            }
        }
        return latest;
    }

    /**
     * The last-departure times at exactly the given station, resolved to display names.
     *
     * @param lineName     Turns a line id into the name to show for it.
     * @param categoryName Turns a category id into the name to show for it.
     */
    public DepartureStats getStats(String stationName, Function<UUID, String> lineName, Function<UUID, String> categoryName) {
        Departures departures = departuresByStation.get(stationName);
        if (departures == null) {
            return DepartureStats.empty();
        }
        Map<String, Long> categories = new HashMap<>();
        departures.byCategory.forEach((id, time) -> {
            String name = categoryName.apply(id);
            if (name != null) {
                categories.put(name, time);
            }
        });
        Map<String, Long> lines = new HashMap<>();
        departures.byLine.forEach((id, time) -> {
            String name = lineName.apply(id);
            if (name != null) {
                lines.put(name, time);
            }
        });
        return new DepartureStats(departures.last, categories, lines, new HashMap<>(departures.byName));
    }

    /** Removes the records of all stations not in the given collection. */
    public void retainStations(Collection<String> existingStations) {
        departuresByStation.keySet().retainAll(existingStations);
    }

    /** Removes all recorded departures. */
    public void clear() {
        departuresByStation.clear();
    }

    /** The number of stations departures have been recorded for. */
    public int getStationCount() {
        return departuresByStation.size();
    }

    /** Serializes the recorded departures. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        departuresByStation.forEach((station, departures) -> nbt.put(station, departures.toNbt()));
        return nbt;
    }

    /** Restores persisted departures. */
    public void loadNbt(CompoundTag nbt) {
        departuresByStation.clear();
        for (String station : nbt.getAllKeys()) {
            departuresByStation.put(station, Departures.fromNbt(nbt.getCompound(station)));
        }
    }
}
