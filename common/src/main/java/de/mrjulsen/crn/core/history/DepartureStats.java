package de.mrjulsen.crn.core.history;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;

/**
 * Summary figures about the departures recorded at a station: when trains of each line, category and
 * name last left. Times are in game ticks on the backend's time base.
 *
 * @param lastDeparture       When any train last departed, or a negative value if none has.
 * @param departuresByCategory The last departure time keyed by category name.
 * @param departuresByLine     The last departure time keyed by line name.
 * @param departuresByName     The last departure time keyed by train name.
 */
public record DepartureStats(
    long lastDeparture,
    Map<String, Long> departuresByCategory,
    Map<String, Long> departuresByLine,
    Map<String, Long> departuresByName
) {

    private static final String NBT_LAST_DEPARTURE = "LastDeparture";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_LINE = "Line";
    private static final String NBT_NAME = "Name";

    public static DepartureStats empty() {
        return new DepartureStats(-1, Map.of(), Map.of(), Map.of());
    }

    /** Whether nothing has been recorded. */
    public boolean isEmpty() {
        return lastDeparture < 0 && departuresByCategory.isEmpty() && departuresByLine.isEmpty() && departuresByName.isEmpty();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_LAST_DEPARTURE, lastDeparture);
        nbt.put(NBT_CATEGORY, writeMap(departuresByCategory));
        nbt.put(NBT_LINE, writeMap(departuresByLine));
        nbt.put(NBT_NAME, writeMap(departuresByName));
        return nbt;
    }

    public static DepartureStats fromNbt(CompoundTag nbt) {
        return new DepartureStats(
            nbt.getLong(NBT_LAST_DEPARTURE),
            readMap(nbt.getCompound(NBT_CATEGORY)),
            readMap(nbt.getCompound(NBT_LINE)),
            readMap(nbt.getCompound(NBT_NAME))
        );
    }

    private static CompoundTag writeMap(Map<String, Long> map) {
        CompoundTag nbt = new CompoundTag();
        map.forEach(nbt::putLong);
        return nbt;
    }

    private static Map<String, Long> readMap(CompoundTag nbt) {
        Map<String, Long> map = new HashMap<>();
        for (String key : nbt.getAllKeys()) {
            map.put(key, nbt.getLong(key));
        }
        return map;
    }
}
