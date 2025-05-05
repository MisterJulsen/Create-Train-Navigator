package de.mrjulsen.crn.data.train.portable;

import java.util.Objects;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag.ClientStationTag;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;

/** Contains data about one train arrival at a specific station. This data is used by displays and does not provide any additional functionality. */
public class TrainStopDisplayData {
    private final int stationEntryIndex;
    private final ClientStationTag scheduledStation;
    private final ClientStationTag realTimeStation;
    private final long scheduledDepartureTime;
    private final long scheduledArrivalTime;
    private final long realTimeDepartureTime;
    private final long realTimeArrivalTime;
    private final String destination;
    private final String trainName;

    private static final String NBT_STATION_INDEX = "Index";
    private static final String NBT_SCHEDULED_STATION = "ScheduledStation";
    private static final String NBT_REAL_TIME_STATION = "RealTimeStation";
    private static final String NBT_SCHEDULED_DEPARTURE_TIME = "ScheduledDeparture";
    private static final String NBT_SCHEDULED_ARRIVAL_TIME = "ScheduledArrival";
    private static final String NBT_REAL_TIME_DEPARTURE_TIME = "RealTimeArrival";
    private static final String NBT_REAL_TIME_ARRIVAL_TIME = "RealTimeDeparture";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_TRAIN_NAME = "TrainName";

    public TrainStopDisplayData(
        int stationEntryIndex,
        ClientStationTag scheduledStation,
        ClientStationTag realTimeStation,
        long scheduledDepartureTime,
        long scheduledArrivalTime,
        long realTimeDepartureTime,
        long realTimeArrivalTime,
        String destination,
        String trainName
    ) {
        this.stationEntryIndex = stationEntryIndex;
        this.scheduledStation = scheduledStation;
        this.realTimeStation = realTimeStation;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.realTimeDepartureTime = realTimeDepartureTime;
        this.realTimeArrivalTime = realTimeArrivalTime;
        this.destination = destination;
        this.trainName = trainName;
    }

    public static TrainStopDisplayData empty() {
        return new TrainStopDisplayData(-1, ClientStationTag.empty(), ClientStationTag.empty(), 0, 0, 0, 0, "", "");
    }

    /** Server-side only! */
    public static TrainStopDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return new TrainStopDisplayData(
            stop.getScheduleIndex(),
            stop.getScheduledStationTag(),
            stop.getRealTimeStationTag(),
            stop.getScheduledDepartureTime(),
            stop.getScheduledArrivalTime(), 
            stop.getRealTimeDepartureTime(), 
            stop.getRealTimeArrivalTime(), 
            stop.getDisplayTitle(),
            stop.getTrainDisplayName()
        );
    }

    public int getStationEntryIndex() {
        return stationEntryIndex;
    }

    public ClientStationTag getScheduledStation() {
        return scheduledStation;
    }

    public ClientStationTag getRealTimeStation() {
        return realTimeStation;
    }

    public long getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public long getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public long getRealTimeDepartureTime() {
        return realTimeDepartureTime;
    }

    public long getRealTimeArrivalTime() {
        return realTimeArrivalTime;
    }

    public String getDestination() {
        return destination;
    }

    public String getTrainName() {
        return trainName;
    }




    public long getDepartureTimeDeviation() {
        return getRealTimeDepartureTime() - getScheduledDepartureTime();
    }

    public long getArrivalTimeDeviation() {
        return getRealTimeArrivalTime() - getScheduledArrivalTime();
    }

    public boolean isDepartureDelayed() {
        return getDepartureTimeDeviation() > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isArrivalDelayed() {
        return getArrivalTimeDeviation() > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isStationChanged() {
        return !getScheduledStation().equals(getRealTimeStation());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt(NBT_STATION_INDEX, stationEntryIndex);
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_REAL_TIME_STATION, realTimeStation.toNbt());
        nbt.putLong(NBT_SCHEDULED_DEPARTURE_TIME, scheduledDepartureTime);
        nbt.putLong(NBT_SCHEDULED_ARRIVAL_TIME, scheduledArrivalTime);
        nbt.putLong(NBT_REAL_TIME_DEPARTURE_TIME, realTimeDepartureTime);
        nbt.putLong(NBT_REAL_TIME_ARRIVAL_TIME, realTimeArrivalTime);
        nbt.putString(NBT_DESTINATION, destination);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        return nbt;
    }

    public static TrainStopDisplayData fromNbt(CompoundTag nbt) {
        return new TrainStopDisplayData(
            nbt.getInt(NBT_STATION_INDEX),
            nbt.contains(NBT_SCHEDULED_STATION) ? ClientStationTag.fromNbt(nbt.getCompound(NBT_SCHEDULED_STATION)) : ClientStationTag.empty(),
            nbt.contains(NBT_REAL_TIME_STATION) ? ClientStationTag.fromNbt(nbt.getCompound(NBT_REAL_TIME_STATION)) : ClientStationTag.empty(),
            nbt.getLong(NBT_SCHEDULED_DEPARTURE_TIME),
            nbt.getLong(NBT_SCHEDULED_ARRIVAL_TIME), 
            nbt.getLong(NBT_REAL_TIME_DEPARTURE_TIME), 
            nbt.getLong(NBT_REAL_TIME_ARRIVAL_TIME),
            nbt.getString(NBT_DESTINATION), 
            nbt.getString(NBT_TRAIN_NAME)
        );
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof TrainStopDisplayData o && o.getDestination().equals(getDestination()) && o.getStationEntryIndex() == getStationEntryIndex();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getDestination(), getStationEntryIndex());
    }
}
