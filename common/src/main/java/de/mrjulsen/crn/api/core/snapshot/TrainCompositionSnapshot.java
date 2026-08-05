package de.mrjulsen.crn.api.core.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.nbt.CompoundTag;

/**
 * What a train is made of, carriage by carriage, as it stood when the snapshot was taken.
 *
 * @param trainId           The train this describes.
 * @param carriages         Its carriages, ordered from the front.
 * @param totalLengthBlocks The whole train's length in blocks, carriages and the gaps between them
 *                          taken together.
 * @param doubleEnded       Whether the train can be driven from either end.
 * @param backwards         Whether the train is currently running in reverse, which inverts the
 *                          relation between carriage order and direction of travel.
 */
public record TrainCompositionSnapshot(
    @ResponseAlwaysInclude UUID trainId,
    List<CarriageSnapshot> carriages,
    int totalLengthBlocks,
    boolean doubleEnded,
    boolean backwards
) {

    public TrainCompositionSnapshot {
        carriages = carriages == null ? List.of() : List.copyOf(carriages);
    }

    public static TrainCompositionSnapshot of(Train train) {
        List<CarriageSnapshot> carriages = new ArrayList<>();
        int length = 0;

        if (train.carriages != null) {
            for (int i = 0; i < train.carriages.size(); i++) {
                Carriage carriage = train.carriages.get(i);
                if (carriage == null) {
                    continue;
                }
                carriages.add(CarriageSnapshot.of(carriage, i));
                length += carriage.bogeySpacing;
            }
        }
        if (train.carriageSpacing != null) {
            for (Integer spacing : train.carriageSpacing) {
                length += spacing == null ? 0 : spacing;
            }
        }

        return new TrainCompositionSnapshot(train.id, carriages, length, train.doubleEnded, train.currentlyBackwards);
    }

    public int carriageCount() {
        return carriages.size();
    }

    /** Whether any carriage cannot move, which holds the whole train up. */
    public boolean hasStalledCarriage() {
        return carriages.stream().anyMatch(CarriageSnapshot::stalled);
    }

    /** Whether any carriage provides storage. */
    public boolean hasStorage() {
        return carriages.stream().anyMatch(CarriageSnapshot::hasStorage);
    }

    /** Whether the train currently reaches across a portal into more than one dimension. */
    public boolean spansDimensions() {
        return carriages.stream().anyMatch(CarriageSnapshot::inMultipleDimensions);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        nbt.put(NBT_CARRIAGES, NbtHelper.writeList(carriages, CarriageSnapshot::toNbt));
        nbt.putInt(NBT_TOTAL_LENGTH, totalLengthBlocks);
        nbt.putBoolean(NBT_DOUBLE_ENDED, doubleEnded);
        nbt.putBoolean(NBT_BACKWARDS, backwards);
        return nbt;
    }

    public static TrainCompositionSnapshot fromNbt(CompoundTag nbt) {
        return new TrainCompositionSnapshot(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            NbtHelper.readList(nbt, NBT_CARRIAGES, CarriageSnapshot::fromNbt),
            nbt.getInt(NBT_TOTAL_LENGTH),
            nbt.getBoolean(NBT_DOUBLE_ENDED),
            nbt.getBoolean(NBT_BACKWARDS)
        );
    }

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_CARRIAGES = "Carriages";
    private static final String NBT_TOTAL_LENGTH = "TotalLengthBlocks";
    private static final String NBT_DOUBLE_ENDED = "DoubleEnded";
    private static final String NBT_BACKWARDS = "Backwards";
}
