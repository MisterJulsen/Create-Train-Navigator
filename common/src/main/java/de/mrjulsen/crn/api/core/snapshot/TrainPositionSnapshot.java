package de.mrjulsen.crn.api.core.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Where a train is and how fast it is going, taken from its leading carriage.
 * <p>
 * Position is only known while the train's chunks are loaded; otherwise the dimension and position
 * are {@code null}. Speeds are in blocks per tick, using the same unit as Create's train speeds,
 * and are negative when the train runs in reverse.
 *
 * @param trainId            The train this describes.
 * @param dimension          The dimension the leading carriage is in, or {@code null}.
 * @param position           Where the leading carriage is, or {@code null}.
 * @param dimensions         Every dimension the leading carriage is present in, which holds more
 *                           than one entry only while it spans a portal.
 * @param speed              The train's current speed.
 * @param targetSpeed        The speed the train is working towards.
 * @param maxSpeed           The fastest the train may go.
 * @param throttle           The throttle setting, from zero to one.
 * @param backwards          Whether the train is running in reverse.
 * @param distanceToNextStop How far the train still has to travel to its destination, in blocks, or
 *                           {@code -1} if it is not navigating.
 * @param exitSide           Which side of the train the doors face at its destination.
 */
public record TrainPositionSnapshot(
    @ResponseAlwaysInclude UUID trainId,
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

    /** A placeholder for a train whose whereabouts cannot be determined. */
    public static TrainPositionSnapshot unknown(UUID trainId) {
        return new TrainPositionSnapshot(trainId, null, null, List.of(), 0, 0, 0, 0, false, -1, TrainExitSide.UNKNOWN);
    }

    public TrainPositionSnapshot {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        exitSide = exitSide == null ? TrainExitSide.UNKNOWN : exitSide;
    }

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

    /** Whether the train's whereabouts are known, which requires its chunks to be loaded. */
    public boolean isLoaded() {
        return position != null;
    }

    /** Whether the train is moving, disregarding speeds too small to matter. */
    public boolean isMoving() {
        return Math.abs(speed) > 0.001;
    }

    /** How fast the train is going as a fraction of its maximum, from zero to one. */
    public double speedFraction() {
        return maxSpeed <= 0 ? 0 : Math.min(1, Math.abs(speed) / maxSpeed);
    }

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
}
