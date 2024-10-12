package de.mrjulsen.crn.debug;

import java.util.UUID;

import de.mrjulsen.crn.data.train.TrainData;
import net.minecraft.nbt.CompoundTag;

public record TrainDebugData(
    UUID sessionId,
    UUID trainId,
    String trainName,
    int totalDuration,
    int predictionsCount,
    int predictionsInitialized,
    TrainDebugState state
) {

    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_DURATION = "Duration";
    private static final String NBT_PREDICTIONS = "Predictions";
    private static final String NBT_INITIALIZED_PREDICTIONS = "InitializedPredictions";
    private static final String NBT_STATE = "State";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_SESSION_ID, sessionId);
        nbt.putUUID(NBT_ID, trainId);
        nbt.putString(NBT_NAME, trainName);
        nbt.putInt(NBT_DURATION, totalDuration);
        nbt.putInt(NBT_PREDICTIONS, predictionsCount);
        nbt.putInt(NBT_INITIALIZED_PREDICTIONS, predictionsInitialized);
        nbt.putByte(NBT_STATE, state.getId());
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
            TrainDebugState.getStateById(nbt.getByte(NBT_STATE))
        );
    }

    public static TrainDebugData fromTrain(TrainData train) {
        return new TrainDebugData(
            train.getSessionId(),
            train.getTrainId(),
            train.getTrainName(),
            train.getTotalDuration(),
            train.getPredictionsRaw().size(),
            train.debug_initializedStationsCount(),
            train.isPreparing() ? TrainDebugState.PREPARING : (train.isInitialized() ? TrainDebugState.READY : TrainDebugState.INITIALIZING)
        );
    }
}
