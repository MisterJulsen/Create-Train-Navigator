package de.mrjulsen.crn.backend.history;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;

/**
 * A station's last-departure times as a display needs them: the most recent departure overall, and
 * the most recent one per category, per line and per train name - each keyed by the display name and
 * carrying the time it happened.
 * <p>
 * Built server-side from a {@link DepartureLog} and sent to the client for the station's goggle
 * tooltip, so the client never has to resolve line or category ids of its own.
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
