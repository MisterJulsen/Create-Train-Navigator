package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of a train category: the category itself plus which trains and lines
 * currently carry it.
 *
 * @param category      The train category itself.
 * @param trainIds      The trains currently operating under this category.
 * @param lines         The train lines currently operating under this category.
 * @param delayedTrains How many of its trains are currently late.
 */
public record CategorySnapshot(
    CategoryRef category,
    Set<UUID> trainIds,
    List<LineRef> lines,
    int delayedTrains
) {

    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_TRAIN_IDS = "TrainIds";
    private static final String NBT_LINES = "Lines";
    private static final String NBT_DELAYED_TRAINS = "DelayedTrains";

    public CategorySnapshot {
        category = category == null ? CategoryRef.NONE : category;
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** The category's id. */
    public UUID id() {
        return category.id();
    }

    /** The category's name. */
    public String name() {
        return category.name();
    }

    /** How many trains currently operate under this category. */
    public int trainCount() {
        return trainIds.size();
    }

    /** Whether any train currently operates under this category. */
    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    /** Whether any train of this category is currently late. */
    public boolean hasDelays() {
        return delayedTrains > 0;
    }

    /** Serializes this category. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.put(NBT_TRAIN_IDS, NbtHelper.writeUuids(trainIds));
        nbt.put(NBT_LINES, NbtHelper.writeList(lines, LineRef::toNbt));
        nbt.putInt(NBT_DELAYED_TRAINS, delayedTrains);
        return nbt;
    }

    /** Deserializes a category written by {@link #toNbt()}. */
    public static CategorySnapshot fromNbt(CompoundTag nbt) {
        return new CategorySnapshot(
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            NbtHelper.readUuids(nbt, NBT_TRAIN_IDS),
            NbtHelper.readList(nbt, NBT_LINES, LineRef::fromNbt),
            nbt.getInt(NBT_DELAYED_TRAINS)
        );
    }
}
