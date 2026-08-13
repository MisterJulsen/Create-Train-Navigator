package de.mrjulsen.crn.api.core.snapshot;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * A category together with the trains currently running under it and the lines it covers. Only
 * trains fit to be reported are counted.
 *
 * @param category      The category this describes.
 * @param trainIds      The ids of the trains currently running under it.
 * @param lines         The lines those trains work while in this category.
 * @param delayedTrains How many of those trains are running late.
 */
public record CategorySnapshot(
    TrainCategoryRef category,
    Set<UUID> trainIds,
    List<LineRef> lines,
    int delayedTrains
) {

    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_TRAIN_IDS = "TrainIds";
    private static final String NBT_LINES = "Lines";
    private static final String NBT_DELAYED_TRAINS = "DelayedTrains";

    public CategorySnapshot {
        category = category == null ? TrainCategoryRef.NONE : category;
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public UUID id() {
        return category.id();
    }

    public String name() {
        return category.name();
    }

    public int trainCount() {
        return trainIds.size();
    }

    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    public boolean hasDelays() {
        return delayedTrains > 0;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.put(NBT_TRAIN_IDS, NbtHelper.writeUuids(trainIds));
        nbt.put(NBT_LINES, NbtHelper.writeList(lines, LineRef::toNbt));
        nbt.putInt(NBT_DELAYED_TRAINS, delayedTrains);
        return nbt;
    }

    public static CategorySnapshot fromNbt(CompoundTag nbt) {
        return new CategorySnapshot(
            TrainCategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            NbtHelper.readUuids(nbt, NBT_TRAIN_IDS),
            NbtHelper.readList(nbt, NBT_LINES, LineRef::fromNbt),
            nbt.getInt(NBT_DELAYED_TRAINS)
        );
    }
}
