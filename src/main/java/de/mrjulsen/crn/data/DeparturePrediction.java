package de.mrjulsen.crn.data;

import java.util.Objects;
import java.util.UUID;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.event.listeners.TrainListener;
import net.minecraft.nbt.CompoundTag;

public class DeparturePrediction {

    private Train train;
    private int ticks;
    private String scheduleTitle;
    private String nextStop;

    private int cycle;

    public DeparturePrediction(Train train, int ticks, String scheduleTitle, String nextStop, int cycle) {
        this.train = train;
        this.ticks = ticks;
        this.scheduleTitle = scheduleTitle;
        this.nextStop = nextStop;
        this.cycle = cycle;
    }
    
    public DeparturePrediction(TrainDeparturePrediction prediction) {
        this(prediction.train, prediction.ticks, prediction.scheduleTitle.getString(), prediction.destination, 0);
    }

    public DeparturePrediction copy() {
        return new DeparturePrediction(getTrain(), getTicks(), getScheduleTitle(), getNextStopStation(), getCycle());
    }

    public static DeparturePrediction withNextCycleTicks(DeparturePrediction current) {
        int cycle = current.getCycle() + 1;
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycle) + current.getTicks(), current.getScheduleTitle(), current.getNextStopStation(), cycle);
    }

    public static DeparturePrediction withCycleTicks(DeparturePrediction current, int cycles) {
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycles) + current.getTicks(), current.getScheduleTitle(), current.getNextStopStation(), cycles);
    }

    public Train getTrain() {
        return train;
    }

    public int getTicks() {
        return ticks;
    }

    public String getScheduleTitle() {
        return scheduleTitle;
    }

    public String getNextStopStation() {
        return nextStop;
    }

    public TrainStationAlias getNextStop() {
        return GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(nextStop);
    }

    public int getCycle() {
        return cycle;
    }

    public int getTrainCycleDuration() {
        return getTrainCycleDuration(getTrain());
    }

    public static int getTrainCycleDuration(Train train) {
        return TrainListener.getInstance().getApproximatedTrainDuration(train);
    }

    public SimpleDeparturePrediction simplify() {
        return new SimpleDeparturePrediction(getNextStop().getAliasName().get(), getTicks(), getScheduleTitle(), getTrain().id);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeparturePrediction other) {
            return getTrain().id == other.getTrain().id && getTicks() == other.getTicks() && getScheduleTitle().equals(other.getScheduleTitle()) && getNextStopStation().equals(other.getNextStopStation());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 19 * Objects.hash(getTrain().id, getTicks(), getScheduleTitle(), getNextStopStation());
    }

    public boolean similar(DeparturePrediction other) {
        return getTicks() == other.getTicks() && getNextStopStation().equals(other.getNextStopStation());
    }

    @Override
    public String toString() {
        return String.format("%s, Next stop: %s in %st", getTrain().name.getString(), getNextStop().getAliasName(), getTicks());
    }

    public static record SimpleDeparturePrediction(String station, int ticks, String scheduleTitle, UUID id) {

        private static final String NBT_STATION = "station";
        private static final String NBT_TICKS = "ticks";
        private static final String NBT_SCHEDULE_TITLE = "title";
        private static final String NBT_ID = "id";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_STATION, station);
            nbt.putInt(NBT_TICKS, ticks);
            nbt.putString(NBT_SCHEDULE_TITLE, scheduleTitle);
            nbt.putUUID(NBT_ID, id);

            return nbt;
        }

        public static SimpleDeparturePrediction fromNbt(CompoundTag nbt) {
            return new SimpleDeparturePrediction(
                nbt.getString(NBT_STATION), 
                nbt.getInt(NBT_TICKS), 
                nbt.getString(NBT_SCHEDULE_TITLE), 
                nbt.getUUID(NBT_ID)
            );
        }
    }
}
