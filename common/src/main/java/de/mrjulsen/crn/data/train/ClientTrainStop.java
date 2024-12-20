package de.mrjulsen.crn.data.train;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.StationTag.ClientStationTag;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.crn.data.navigation.ITrainListenerClient;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.util.IListenable;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A small variant of the {@code NewTrainPrediction} class, representing a stop of a train on its route with some important information.
 */
public class ClientTrainStop extends TrainStop implements ITrainListenerClient<ClientTrainStop.TrainStopRealTimeData>, IListenable<ClientTrainStop> {

    public static final String EVENT_UPDATE = "update";
    public static final String EVENT_DELAY = "delayed";
    public static final String EVENT_SCHEDULE_CHANGED = "schedule_changed";
    public static final String EVENT_STATION_CHANGED = "station_changed";
    public static final String EVENT_STATION_REACHED = "station_reached";
    public static final String EVENT_STATION_LEFT = "station_left";
    public static final String EVENT_ANNOUNCE_NEXT_STOP = "announce_next_stop";
    private final Map<String, IdentityHashMap<Object, Consumer<ClientTrainStop>>> listeners = new HashMap<>();
    
    private boolean isClosed = false;

    public ClientTrainStop(int scheduleIndex, int sectionIndex, UUID trainId, String trainName, TrainIconType trainIcon, TrainInfo trainInfo,
            String scheduleTitle, boolean isCustomTitle, String terminusText, int stayDuration, boolean simulated,
            long scheduledDepartureTime, long scheduledArrivalTime, int cycle, ClientStationTag tag, long realTimeArrivalTime,
            long realTimeDepartureTime, int realTimeCycle, ClientStationTag realTimeTag, long arrivalTimeDeviation,
            long departureTimeDeviation, int realTimeTicksUntilArrival, TrainState trainPosition)
    {
        super(scheduleIndex, sectionIndex, trainId, trainName, trainIcon, trainInfo, scheduleTitle, isCustomTitle,
                terminusText, stayDuration, simulated, scheduledDepartureTime, scheduledArrivalTime, cycle, tag,
                realTimeArrivalTime, realTimeDepartureTime, realTimeCycle, realTimeTag, arrivalTimeDeviation,
                departureTimeDeviation, realTimeTicksUntilArrival, trainPosition);
        initEvents();
    }

    private void initEvents() {
        this.createEvent(EVENT_UPDATE);
        this.createEvent(EVENT_DELAY);
        this.createEvent(EVENT_SCHEDULE_CHANGED);
        this.createEvent(EVENT_STATION_CHANGED);
        this.createEvent(EVENT_STATION_REACHED);
        this.createEvent(EVENT_STATION_LEFT);
        this.createEvent(EVENT_ANNOUNCE_NEXT_STOP);
    }

    @Override
    public Map<String, IdentityHashMap<Object, Consumer<ClientTrainStop>>> getListeners() {
        return listeners;
    }

    /** Client-side only! */
    public long getRoundedRealTimeArrivalTime() throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        return (getScheduledArrivalTime() + getArrivalTimeDeviation()) / ModClientConfig.REALTIME_PRECISION_THRESHOLD.get() * ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();
    }

    /** Client-side only! */
    public long getRoundedRealTimeDepartureTime() throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        return (getScheduledDepartureTime() + getDepartureTimeDeviation()) / ModClientConfig.REALTIME_PRECISION_THRESHOLD.get() * ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();
    }

    @Override
    public void update(TrainStopRealTimeData data) {
        if (isClosed) {
            return;
        }

        if (data.cycle() != getScheduledCycle()) {
            if (data.cycle() > getScheduledCycle() && trainState != TrainState.AFTER) {
                trainState = TrainState.AFTER;
                notifyListeners(EVENT_STATION_LEFT, this);
                close();
            }
            return;
        }

        boolean wasDelayed = isDepartureDelayed();
        String oldRealTimeStation = getRealTimeStationTag().stationName();
        int oldTimeUntilArrival = getTicksUntilArrival();

        if (scheduledArrivalTime != data.scheduledArrivalTime() || scheduledDepartureTime != data.scheduledDepartureTime()) {
            notifyListeners(EVENT_SCHEDULE_CHANGED, this);
        }

        this.scheduledArrivalTime = data.scheduledArrivalTime();
        this.scheduledDepartureTime = data.scheduledDepartureTime();
        this.realTimeArrivalTime = data.realTimeArrivalTime();
        this.realTimeDepartureTime = data.realTimeDepartureTime();
        this.arrivalTimeDeviation = data.deltaArrivalTime();
        this.departureTimeDeviation = data.deltaDepartureTime();
        this.realTimeCycle = data.cycle();
        this.realTimeTag = data.station();
        this.realTimeTicksUntilArrival = data.ticksUntilArrival();

        if (!wasDelayed && isAnyDelayed()) {
            notifyListeners(EVENT_DELAY, this);
        }
        if (!oldRealTimeStation.equals(getRealTimeStationTag().stationName())) {
            notifyListeners(EVENT_STATION_CHANGED, this);
        }
        if (trainState == TrainState.BEFORE && oldTimeUntilArrival > getTicksUntilArrival() && getTicksUntilArrival() <= ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
            trainState = TrainState.ANNOUNCED;
            notifyListeners(EVENT_ANNOUNCE_NEXT_STOP, this);
        }

        if (trainState.getPositionMultiplier() < 0 && getTicksUntilArrival() <= 0) {
            trainState = TrainState.STAYING;
            notifyListeners(EVENT_STATION_REACHED, this);
        }
        
        notifyListeners(EVENT_UPDATE, this);
    }

    public static TrainStop fromNbt(CompoundTag nbt) {
        return new ClientTrainStop(
            nbt.getInt(NBT_SCHEDULE_INDEX),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getUUID(NBT_TRAIN_ID), 
            nbt.getString(NBT_TRAIN_NAME), 
            TrainIconType.byId(new ResourceLocation(nbt.getString(NBT_TRAIN_ICON))), 
            TrainInfo.fromNbt(nbt.getCompound(NBT_TRAIN_INFO)),
            nbt.getString(NBT_SCHEDULE_TITLE), 
            nbt.getBoolean(NBT_IS_CUSTOM_TITLE), 
            nbt.getString(NBT_TERMINUS_TEXT), 
            nbt.getInt(NBT_STAY_DURATION),
            nbt.getLong(NBT_SIMULATED_TIME) != 0, 
            nbt.getLong(NBT_SCHEDULED_DEPARTURE_TIME), 
            nbt.getLong(NBT_SCHEDULED_ARRIVAL_TIME),
            nbt.getInt(NBT_CYCLE), 
            ClientStationTag.fromNbt(nbt.getCompound(NBT_TAG)),
            nbt.getLong(NBT_REAL_TIME_ARRIVAL_TIME),
            nbt.getLong(NBT_REAL_TIME_DEPARTURE_TIME),
            nbt.getInt(NBT_REAL_CYCLE),
            ClientStationTag.fromNbt(nbt.getCompound(NBT_REAL_TIME_TAG)),
            nbt.contains(NBT_REAL_TIME_ARRIVAL_TIME) ? nbt.getLong(NBT_REAL_TIME_ARRIVAL_TIME) - nbt.getLong(NBT_SCHEDULED_ARRIVAL_TIME) : 0,
            nbt.contains(NBT_REAL_TIME_DEPARTURE_TIME) ? nbt.getLong(NBT_REAL_TIME_DEPARTURE_TIME) - nbt.getLong(NBT_SCHEDULED_DEPARTURE_TIME) : 0,
            0,
            TrainState.BEFORE
        );
    }

    @Override
    public void close() {
        clearEvents();
        isClosed = true;
    }
    
    public static record TrainStopRealTimeData(ClientStationTag station, int entryIndex, long scheduledArrivalTime, long scheduledDepartureTime, long realTimeArrivalTime, long realTimeDepartureTime, long deltaArrivalTime, long deltaDepartureTime, int ticksUntilArrival, int cycle) {
        private static final String NBT_INDEX = "Index";
        private static final String NBT_STATION = "Station";
        private static final String NBT_SCHEDULED_ARRIVAL = "Arrival";
        private static final String NBT_SCHEDULED_DEPARTURE = "Departure";
        private static final String NBT_REAL_TIME_ARRIVAL = "RealArrival";
        private static final String NBT_REAL_TIME_DEPARTURE = "RealDeparture";
        private static final String NBT_DELTA_ARRIVAL = "DeltaArrival";
        private static final String NBT_DELTA_DEPARTURE = "DeltaDeparture";
        private static final String NBT_CYCLE = "Cycle";
        private static final String NBT_TICKS_UNTIL_ARRIVAL = "TUA";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put(NBT_STATION, station.toNbt());
            nbt.putInt(NBT_INDEX, entryIndex);
            nbt.putLong(NBT_SCHEDULED_ARRIVAL, scheduledArrivalTime);
            nbt.putLong(NBT_SCHEDULED_DEPARTURE, scheduledDepartureTime);
            nbt.putLong(NBT_REAL_TIME_ARRIVAL, realTimeArrivalTime);
            nbt.putLong(NBT_REAL_TIME_DEPARTURE, realTimeDepartureTime);
            nbt.putLong(NBT_DELTA_ARRIVAL, deltaArrivalTime);
            nbt.putLong(NBT_DELTA_DEPARTURE, deltaDepartureTime);
            nbt.putInt(NBT_CYCLE, cycle);
            nbt.putInt(NBT_TICKS_UNTIL_ARRIVAL, ticksUntilArrival);
            
            return nbt;
        }

        public static TrainStopRealTimeData fromNbt(CompoundTag nbt) {
            return new TrainStopRealTimeData(
                ClientStationTag.fromNbt(nbt.getCompound(NBT_STATION)),
                nbt.getInt(NBT_INDEX),
                nbt.getLong(NBT_SCHEDULED_ARRIVAL),
                nbt.getLong(NBT_SCHEDULED_DEPARTURE),
                nbt.getLong(NBT_REAL_TIME_ARRIVAL),
                nbt.getLong(NBT_REAL_TIME_DEPARTURE),
                nbt.getLong(NBT_DELTA_ARRIVAL),
                nbt.getLong(NBT_DELTA_DEPARTURE),
                nbt.getInt(NBT_TICKS_UNTIL_ARRIVAL),
                nbt.getInt(NBT_CYCLE)
            );
        }
    }

    public boolean isClosed() {
        return isClosed;
    }
}
