package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * An immutable snapshot of where a train is and how fast it is moving.
 * <p>
 * Deliberately small, so a consumer tracking many trains at once - a live map, say - can poll
 * positions without building a full {@link TrainSnapshot} per train.
 *
 * @param trainId       The id of the train.
 * @param dimension     The dimension the train's leading carriage is in, or {@code null} if the
 *                      train is not loaded anywhere.
 * @param position      The position of the leading carriage, or {@code null} if it is not loaded.
 * @param dimensions    Every dimension the train currently occupies. Usually one, more while it
 *                      spans a portal.
 * @param speed         The current speed in blocks per tick. Negative when running backwards.
 * @param targetSpeed   The speed the train is currently accelerating or braking towards.
 * @param maxSpeed      The highest speed this train could reach, in blocks per tick.
 * @param throttle      How much of its maximum speed the train is currently allowed to use, from
 *                      {@code 0} to {@code 1}.
 * @param backwards     Whether the train is running backwards along its carriage order.
 * @param distanceToNextStop The remaining distance to the train's current destination in blocks, or
 *                      {@code -1} if it is not navigating anywhere.
 * @param exitSide      Which side of the train the platform will be on at its next stop, so a
 *                      passenger information display can say where the doors open. Measured here
 *                      because working it out needs the loaded track and the server thread, neither
 *                      of which a display has.
 */
public record TrainPositionSnapshot(
    UUID trainId,
    ResourceLocation dimension,
    BlockPos position,
    List<ResourceLocation> dimensions,
    double speed,
    double targetSpeed,
    double maxSpeed,
    double throttle,
    boolean backwards,
    double distanceToNextStop,
    TrainExitSide exitSide
) {

    /** A snapshot for a train whose position cannot be determined. */
    public static TrainPositionSnapshot unknown(UUID trainId) {
        return new TrainPositionSnapshot(trainId, null, null, List.of(), 0, 0, 0, 0, false, -1, TrainExitSide.UNKNOWN);
    }

    public TrainPositionSnapshot {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        exitSide = exitSide == null ? TrainExitSide.UNKNOWN : exitSide;
    }

    /**
     * Captures the position of the given train.
     *
     * @param exitSide Which side the doors will open on at its next stop. Passed in rather than
     *                 worked out here: it can only be measured on the server thread, and this is
     *                 built wherever a snapshot happens to be asked for - see
     *                 {@link de.mrjulsen.crn.backend.core.TrackedTrain#getExitSide()}.
     */
    public static TrainPositionSnapshot of(Train train, TrainExitSide exitSide) {
        Carriage leading = train.carriages == null || train.carriages.isEmpty() ? null : train.carriages.get(0);
        if (leading == null) {
            return unknown(train.id);
        }

        List<ResourceKey<Level>> present = leading.getPresentDimensions();
        ResourceLocation dimension = present.isEmpty() ? null : present.get(0).location();
        BlockPos position = present.isEmpty() ? null : leading.getPositionInDimension(present.get(0)).orElse(null);

        return new TrainPositionSnapshot(
            train.id,
            dimension,
            position,
            present.stream().map(ResourceKey::location).toList(),
            train.speed,
            train.targetSpeed,
            train.maxSpeed(),
            train.throttle,
            train.currentlyBackwards,
            train.navigation == null || train.navigation.destination == null ? -1 : train.navigation.distanceToDestination,
            exitSide
        );
    }

    /** Whether the train's position is currently known. */
    public boolean isLoaded() {
        return position != null;
    }

    /** Whether the train is moving at all. */
    public boolean isMoving() {
        return Math.abs(speed) > 0.001;
    }

    /** The current speed as a fraction of the train's maximum, from {@code 0} to {@code 1}. */
    public double speedFraction() {
        return maxSpeed <= 0 ? 0 : Math.min(1, Math.abs(speed) / maxSpeed);
    }

    /** Serializes this position. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        if (dimension != null) {
            nbt.putString(NBT_DIMENSION, dimension.toString());
        }
        if (position != null) {
            nbt.putLong(NBT_POSITION, position.asLong());
        }
        ListTag dimensionsTag = new ListTag();
        for (ResourceLocation id : dimensions) {
            dimensionsTag.add(StringTag.valueOf(id.toString()));
        }
        nbt.put(NBT_DIMENSIONS, dimensionsTag);
        nbt.putDouble(NBT_SPEED, speed);
        nbt.putDouble(NBT_TARGET_SPEED, targetSpeed);
        nbt.putDouble(NBT_MAX_SPEED, maxSpeed);
        nbt.putDouble(NBT_THROTTLE, throttle);
        nbt.putBoolean(NBT_BACKWARDS, backwards);
        nbt.putDouble(NBT_DISTANCE, distanceToNextStop);
        nbt.putByte(NBT_EXIT_SIDE, exitSide.getAsByte());
        return nbt;
    }

    /** Deserializes a position written by {@link #toNbt()}. */
    public static TrainPositionSnapshot fromNbt(CompoundTag nbt) {
        ListTag dimensionsTag = nbt.getList(NBT_DIMENSIONS, Tag.TAG_STRING);
        List<ResourceLocation> dimensions = new ArrayList<>(dimensionsTag.size());
        for (int i = 0; i < dimensionsTag.size(); i++) {
            dimensions.add(new ResourceLocation(dimensionsTag.getString(i)));
        }

        return new TrainPositionSnapshot(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            nbt.contains(NBT_DIMENSION) ? new ResourceLocation(nbt.getString(NBT_DIMENSION)) : null,
            nbt.contains(NBT_POSITION) ? BlockPos.of(nbt.getLong(NBT_POSITION)) : null,
            dimensions,
            nbt.getDouble(NBT_SPEED),
            nbt.getDouble(NBT_TARGET_SPEED),
            nbt.getDouble(NBT_MAX_SPEED),
            nbt.getDouble(NBT_THROTTLE),
            nbt.getBoolean(NBT_BACKWARDS),
            nbt.contains(NBT_DISTANCE) ? nbt.getDouble(NBT_DISTANCE) : -1,
            TrainExitSide.getFromByte(nbt.getByte(NBT_EXIT_SIDE))
        );
    }

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_DIMENSION = "Dimension";
    private static final String NBT_POSITION = "Position";
    private static final String NBT_DIMENSIONS = "Dimensions";
    private static final String NBT_SPEED = "Speed";
    private static final String NBT_TARGET_SPEED = "TargetSpeed";
    private static final String NBT_MAX_SPEED = "MaxSpeed";
    private static final String NBT_THROTTLE = "Throttle";
    private static final String NBT_BACKWARDS = "Backwards";
    private static final String NBT_DISTANCE = "DistanceToNextStop";
    private static final String NBT_EXIT_SIDE = "ExitSide";
}
