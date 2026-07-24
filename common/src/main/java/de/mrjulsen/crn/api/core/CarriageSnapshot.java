package de.mrjulsen.crn.api.core;

import java.util.List;

import com.simibubi.create.content.trains.entity.Carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * One carriage of a train as it stood when the snapshot was taken.
 * <p>
 * A carriage spanning a portal is present in more than one dimension; {@code dimension} and
 * {@code position} then report only the first of them.
 *
 * @param index                Position within the train, counted from the front, starting at zero.
 * @param lengthBlocks         The spacing between the carriage's bogeys, in blocks.
 * @param onTwoBogeys          Whether the carriage rests on two bogeys rather than one.
 * @param dimension            The dimension the carriage is in, or {@code null} if it is not
 *                             loaded.
 * @param position             Where the carriage is, or {@code null} if it is not loaded.
 * @param blocked              Whether something is obstructing the carriage.
 * @param stalled              Whether the carriage cannot move.
 * @param hasConductor         Whether a conductor is aboard at either end.
 * @param hasStorage           Whether the carriage provides storage.
 * @param inMultipleDimensions Whether the carriage currently spans more than one dimension.
 */
public record CarriageSnapshot(
    int index,
    int lengthBlocks,
    boolean onTwoBogeys,
    ResourceLocation dimension,
    BlockPos position,
    boolean blocked,
    boolean stalled,
    boolean hasConductor,
    boolean hasStorage,
    boolean inMultipleDimensions
) {

    public static CarriageSnapshot of(Carriage carriage, int index) {
        ResourceLocation dimension = null;
        BlockPos position = null;

        List<ResourceKey<Level>> dimensions = carriage.getPresentDimensions();
        if (!dimensions.isEmpty()) {
            ResourceKey<Level> key = dimensions.get(0);
            dimension = key.location();
            position = carriage.getPositionInDimension(key).orElse(null);
        }

        return new CarriageSnapshot(
            index,
            carriage.bogeySpacing,
            carriage.isOnTwoBogeys(),
            dimension,
            position,
            carriage.blocked,
            carriage.stalled,
            carriage.presentConductors != null && (carriage.presentConductors.getFirst() || carriage.presentConductors.getSecond()),
            carriage.storage != null,
            carriage.presentInMultipleDimensions()
        );
    }

    /** Whether the carriage's whereabouts are known, which requires its chunks to be loaded. */
    public boolean isLoaded() {
        return position != null;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_INDEX, index);
        nbt.putInt(NBT_LENGTH, lengthBlocks);
        nbt.putBoolean(NBT_TWO_BOGEYS, onTwoBogeys);
        if (dimension != null) {
            nbt.putString(NBT_DIMENSION, dimension.toString());
        }
        if (position != null) {
            nbt.putLong(NBT_POSITION, position.asLong());
        }
        nbt.putBoolean(NBT_BLOCKED, blocked);
        nbt.putBoolean(NBT_STALLED, stalled);
        nbt.putBoolean(NBT_CONDUCTOR, hasConductor);
        nbt.putBoolean(NBT_STORAGE, hasStorage);
        nbt.putBoolean(NBT_MULTI_DIMENSION, inMultipleDimensions);
        return nbt;
    }

    public static CarriageSnapshot fromNbt(CompoundTag nbt) {
        return new CarriageSnapshot(
            nbt.getInt(NBT_INDEX),
            nbt.getInt(NBT_LENGTH),
            nbt.getBoolean(NBT_TWO_BOGEYS),
            nbt.contains(NBT_DIMENSION) ? new ResourceLocation(nbt.getString(NBT_DIMENSION)) : null,
            nbt.contains(NBT_POSITION) ? BlockPos.of(nbt.getLong(NBT_POSITION)) : null,
            nbt.getBoolean(NBT_BLOCKED),
            nbt.getBoolean(NBT_STALLED),
            nbt.getBoolean(NBT_CONDUCTOR),
            nbt.getBoolean(NBT_STORAGE),
            nbt.getBoolean(NBT_MULTI_DIMENSION)
        );
    }

    private static final String NBT_INDEX = "Index";
    private static final String NBT_LENGTH = "LengthBlocks";
    private static final String NBT_TWO_BOGEYS = "OnTwoBogeys";
    private static final String NBT_DIMENSION = "Dimension";
    private static final String NBT_POSITION = "Position";
    private static final String NBT_BLOCKED = "Blocked";
    private static final String NBT_STALLED = "Stalled";
    private static final String NBT_CONDUCTOR = "HasConductor";
    private static final String NBT_STORAGE = "HasStorage";
    private static final String NBT_MULTI_DIMENSION = "InMultipleDimensions";
}
