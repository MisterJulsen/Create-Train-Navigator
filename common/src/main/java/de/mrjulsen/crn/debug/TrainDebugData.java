package de.mrjulsen.crn.debug;

import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record TrainDebugData(
    UUID sessionId,
    UUID trainId,
    String trainName,
    int totalDuration,
    int predictionsCount,
    int predictionsInitialized,
    TrainDebugState state,
    BlockPos pos,
    ResourceLocation dimension
) {

    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_DURATION = "Duration";
    private static final String NBT_PREDICTIONS = "Predictions";
    private static final String NBT_INITIALIZED_PREDICTIONS = "InitializedPredictions";
    private static final String NBT_STATE = "State";
    private static final String NBT_POSITION = "Position";
    private static final String NBT_DIMENSION = "Dimension";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_SESSION_ID, sessionId);
        nbt.putUUID(NBT_ID, trainId);
        nbt.putString(NBT_NAME, trainName);
        nbt.putInt(NBT_DURATION, totalDuration);
        nbt.putInt(NBT_PREDICTIONS, predictionsCount);
        nbt.putInt(NBT_INITIALIZED_PREDICTIONS, predictionsInitialized);
        nbt.putByte(NBT_STATE, state.getId());
        NbtUtils.putNbtPos(nbt, NBT_POSITION, pos);
        nbt.putString(NBT_DIMENSION, dimension.toString());
        return nbt;
    } 

    public static TrainDebugData fromNbt(CompoundTag nbt) {
        return new TrainDebugData(
                nbt.getUUID(NBT_SESSION_ID),
                nbt.getUUID(NBT_ID),
                nbt.getString(NBT_NAME),
                nbt.getInt(NBT_DURATION),
                nbt.getInt(NBT_PREDICTIONS),
                nbt.getInt(NBT_INITIALIZED_PREDICTIONS),
                TrainDebugState.getStateById(nbt.getByte(NBT_STATE)),
                NbtUtils.getNbtBlockPos(nbt, NBT_POSITION),
                ResourceLocation.tryParse(nbt.getString(NBT_DIMENSION))
        );
    }

    public static TrainDebugData fromTrain(TrainData train) {
        BlockPos pos = BlockPos.ZERO;
        ResourceLocation dimension = ResourceLocation.tryBuild("unknown", "unknown");
        if (train.getTrain() != null) {
            List<ResourceKey<Level>> dimensions = train.getTrain().getPresentDimensions();
            if (!dimensions.isEmpty()) {
                dimension = dimensions.get(0).location();
                pos = train.getTrain().getPositionInDimension(dimensions.get(0)).orElse(BlockPos.ZERO);
            }
        }
        return new TrainDebugData(
                train.getSessionId(),
                train.getTrainId(),
                train.getTrainName(),
                train.getTotalDuration(),
                train.getPredictionsMap().size(),
                train.debug_initializedStationsCount(),
                train.isPreInitializationPhase() ? TrainDebugState.PREPARING : (train.isInitialized() ? TrainDebugState.READY : TrainDebugState.INITIALIZING),
                pos,
                dimension
        );
    }
}
