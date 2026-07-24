package de.mrjulsen.crn.api.core;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * What is known about one station: which tags it belongs to, and which lines, categories and trains
 * call there. Only trains fit to be reported are counted.
 *
 * @param station     The station this describes.
 * @param tags        Every tag covering this station.
 * @param lines       The lines calling here.
 * @param categories  The categories calling here.
 * @param trainIds    The ids of the trains calling here.
 * @param blacklisted Whether the station is hidden from public boards and route searches.
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

    public String name() {
        return station.name();
    }

    /** Whether any train calls here at all. */
    public boolean isServed() {
        return !trainIds.isEmpty();
    }

    public boolean isTagged() {
        return !tags.isEmpty();
    }

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
