package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of a station: what it is called, how it is grouped and which services call
 * there.
 *
 * @param station     The station itself, carrying its primary tag and platform.
 * @param tags        Every station tag this station belongs to. A station usually has one, but
 *                    nothing stops it from being grouped several ways.
 * @param lines       The train lines currently calling at this station.
 * @param categories  The train categories currently calling at this station.
 * @param trainIds    The trains currently scheduled to call at this station.
 * @param blacklisted Whether this station is hidden from public displays.
 */
public record StationSnapshot(
    StationRef station,
    List<TagRef> tags,
    List<LineRef> lines,
    List<CategoryRef> categories,
    Set<UUID> trainIds,
    boolean blacklisted
) {

    private static final String NBT_STATION = "Station";
    private static final String NBT_TAGS = "Tags";
    private static final String NBT_LINES = "Lines";
    private static final String NBT_CATEGORIES = "Categories";
    private static final String NBT_TRAIN_IDS = "TrainIds";
    private static final String NBT_BLACKLISTED = "Blacklisted";

    public StationSnapshot {
        station = station == null ? StationRef.NONE : station;
        tags = tags == null ? List.of() : List.copyOf(tags);
        lines = lines == null ? List.of() : List.copyOf(lines);
        categories = categories == null ? List.of() : List.copyOf(categories);
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
    }

    /** The station's name as it exists in the track network. */
    public String name() {
        return station.name();
    }

    /** Whether any train currently calls at this station. */
    public boolean isServed() {
        return !trainIds.isEmpty();
    }

    /** Whether this station belongs to any tag. */
    public boolean isTagged() {
        return !tags.isEmpty();
    }

    /** Serializes this station. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_STATION, station.toNbt());
        nbt.put(NBT_TAGS, NbtHelper.writeList(tags, TagRef::toNbt));
        nbt.put(NBT_LINES, NbtHelper.writeList(lines, LineRef::toNbt));
        nbt.put(NBT_CATEGORIES, NbtHelper.writeList(categories, CategoryRef::toNbt));
        nbt.put(NBT_TRAIN_IDS, NbtHelper.writeUuids(trainIds));
        nbt.putBoolean(NBT_BLACKLISTED, blacklisted);
        return nbt;
    }

    /** Deserializes a station written by {@link #toNbt()}. */
    public static StationSnapshot fromNbt(CompoundTag nbt) {
        return new StationSnapshot(
            StationRef.fromNbt(nbt.getCompound(NBT_STATION)),
            NbtHelper.readList(nbt, NBT_TAGS, TagRef::fromNbt),
            NbtHelper.readList(nbt, NBT_LINES, LineRef::fromNbt),
            NbtHelper.readList(nbt, NBT_CATEGORIES, CategoryRef::fromNbt),
            NbtHelper.readUuids(nbt, NBT_TRAIN_IDS),
            nbt.getBoolean(NBT_BLACKLISTED)
        );
    }
}
