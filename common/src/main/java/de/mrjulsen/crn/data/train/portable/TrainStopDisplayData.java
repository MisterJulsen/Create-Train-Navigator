package de.mrjulsen.crn.data.train.portable;

import java.util.Objects;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;

/** Contains data about one train arrival at a specific station. This data is used by displays and does not provide any additional functionality. */
public class TrainStopDisplayData {
    private final int stationEntryIndex;
    private final String name;
    private final long scheduledDepartureTime;
    private final long scheduledArrivalTime;
    private final long realTimeDepartureTime;
    private final long realTimeArrivalTime;
    private final String destination;
    private final String trainName;
    private final StationInfo stationInfo;

    private static final String NBT_STATION_INDEX = "Index";
    private static final String NBT_NAME = "Name";
    private static final String NBT_SCHEDULED_DEPARTURE_TIME = "ScheduledDeparture";
    private static final String NBT_SCHEDULED_ARRIVAL_TIME = "ScheduledArrival";
    private static final String NBT_REAL_TIME_DEPARTURE_TIME = "RealTimeArrival";
    private static final String NBT_REAL_TIME_ARRIVAL_TIME = "RealTimeDeparture";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_STATION_INFO = "StationInfo";

    public TrainStopDisplayData(
        int stationEntryIndex,
        String name,
        long scheduledDepartureTime,
        long scheduledArrivalTime,
        long realTimeDepartureTime,
        long realTimeArrivalTime,
        String destination,
        String trainName,
        StationInfo stationInfo
    ) {
        this.stationEntryIndex = stationEntryIndex;
        this.name = name;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.realTimeDepartureTime = realTimeDepartureTime;
        this.realTimeArrivalTime = realTimeArrivalTime;
        this.destination = destination;
        this.trainName = trainName;
        this.stationInfo = stationInfo;
    }

    public static TrainStopDisplayData empty() {
        return new TrainStopDisplayData(-1, "", 0, 0, 0, 0, "", "", StationInfo.empty());
    }

    /** Server-side only! */
    public static TrainStopDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return new TrainStopDisplayData(
            stop.getScheduleIndex(),
            stop.getRealTimeStationTag().tagName(),
            stop.getScheduledDepartureTime(),
            stop.getScheduledArrivalTime(), 
            stop.getRealTimeDepartureTime(), 
            stop.getRealTimeArrivalTime(), 
            stop.getDisplayTitle(),//stop.getRealTimeStationTag().stationName(),
            stop.getTrainName(),
            stop.getRealTimeStationTag().info()
        );
    }

    public int getStationEntryIndex() {
        return stationEntryIndex;
    }

    public String getName() {
        return name;
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

    public StationInfo getStationInfo() {
        return stationInfo;
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

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt(NBT_STATION_INDEX, stationEntryIndex);
        nbt.putString(NBT_NAME, name);
        nbt.putLong(NBT_SCHEDULED_DEPARTURE_TIME, scheduledDepartureTime);
        nbt.putLong(NBT_SCHEDULED_ARRIVAL_TIME, scheduledArrivalTime);
        nbt.putLong(NBT_REAL_TIME_DEPARTURE_TIME, realTimeDepartureTime);
        nbt.putLong(NBT_REAL_TIME_ARRIVAL_TIME, realTimeArrivalTime);
        nbt.putString(NBT_DESTINATION, destination);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        nbt.put(NBT_STATION_INFO, stationInfo.toNbt());
        return nbt;
    }

    public static TrainStopDisplayData fromNbt(CompoundTag nbt) {
        return new TrainStopDisplayData(
            nbt.getInt(NBT_STATION_INDEX),
            nbt.getString(NBT_NAME),
            nbt.getLong(NBT_SCHEDULED_DEPARTURE_TIME),
            nbt.getLong(NBT_SCHEDULED_ARRIVAL_TIME), 
            nbt.getLong(NBT_REAL_TIME_DEPARTURE_TIME), 
            nbt.getLong(NBT_REAL_TIME_ARRIVAL_TIME),
            nbt.getString(NBT_DESTINATION), 
            nbt.getString(NBT_TRAIN_NAME),
            StationInfo.fromNbt(nbt.getCompound(NBT_STATION_INFO))
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
