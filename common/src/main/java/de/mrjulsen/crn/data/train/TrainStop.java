package de.mrjulsen.crn.data.train;

import java.util.UUID;
import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.StationTag.ClientStationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.mcdragonlib.DragonLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A small variant of the {@code NewTrainPrediction} class, representing a stop of a train on its route with some important information.
 */
public class TrainStop implements Comparable<TrainStop> {

    protected static final String NBT_SCHEDULE_INDEX = "ScheduleIndex";
    protected static final String NBT_SECTION_INDEX = "SectionIndex";
    protected static final String NBT_TRAIN_ID = "TrainId";
    protected static final String NBT_TRAIN_NAME = "TrainName";
    protected static final String NBT_TRAIN_ICON = "TrainIcon";
    protected static final String NBT_TRAIN_INFO = "TrainInfo";
    protected static final String NBT_SCHEDULE_TITLE = "ScheduleTitle";
    protected static final String NBT_TERMINUS_TEXT = "TerminusText";
    protected static final String NBT_STAY_DURATION = "StayDuration";
    protected static final String NBT_IS_CUSTOM_TITLE = "IsCustomTitle";
    protected static final String NBT_SIMULATED_TIME = "SimulationTime";

    protected static final String NBT_SCHEDULED_DEPARTURE_TIME = "ScheduledDeparture";
    protected static final String NBT_SCHEDULED_ARRIVAL_TIME = "ScheduledArrival";
    protected static final String NBT_CYCLE = "Cycle";
    protected static final String NBT_TAG = "StationTag";

    protected static final String NBT_REAL_TIME_ARRIVAL_TIME = "RealArrival";
    protected static final String NBT_REAL_TIME_DEPARTURE_TIME = "RealDeparture";
    protected static final String NBT_REAL_CYCLE = "RealCycle";
    protected static final String NBT_REAL_TIME_TAG = "RealTimeTag";
    protected static final String NBT_STATE = "State";

    protected final int scheduleIndex;
    protected final int sectionIndex;
    protected final UUID trainId;
    protected final String trainName;
    protected final TrainIconType trainIcon;
    protected final TrainInfo trainInfo;
    protected final String scheduleTitle;
    protected final String terminusText;
    protected final int stayDuration;
    protected final boolean isCustomTitle;

    protected boolean simulated;
    protected long simulationTime;

    protected long scheduledDepartureTime;
    protected long scheduledArrivalTime;
    protected int cycle; // Der Zyklus, für den dieser Eintrag gültig ist. Wenn der real-time Zyklus kleiner ist, dann ist der Zug noch nicht vorhersagbar, falls größer, ist er abgefahren.
    protected ClientStationTag tag;

    protected long realTimeArrivalTime;
    protected long realTimeDepartureTime;
    protected int realTimeCycle = -1;
    protected ClientStationTag realTimeTag;

    protected long arrivalTimeDeviation;
    protected long departureTimeDeviation;
    protected int realTimeTicksUntilArrival = -1;


    // state
    protected TrainState trainState = TrainState.BEFORE;
    

    public TrainStop(int scheduleIndex, int sectionIndex, UUID trainId, String trainName, TrainIconType trainIcon, TrainInfo trainInfo,
            String scheduleTitle, boolean isCustomTitle, String terminusText, int stayDuration, boolean simulated,
            long scheduledDepartureTime, long scheduledArrivalTime, int cycle, ClientStationTag tag, long realTimeArrivalTime,
            long realTimeDepartureTime, int realTimeCycle, ClientStationTag realTimeTag, long arrivalTimeDeviation,
            long departureTimeDeviation, int realTimeTicksUntilArrival, TrainState trainPosition) {
        this.scheduleIndex = scheduleIndex;
        this.sectionIndex = sectionIndex;
        this.trainId = trainId;
        this.trainName = trainName;
        this.trainIcon = trainIcon;
        this.trainInfo = trainInfo;
        this.scheduleTitle = scheduleTitle;
        this.isCustomTitle = isCustomTitle;
        this.terminusText = terminusText;
        this.stayDuration = stayDuration;
        this.simulated = simulated;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.cycle = cycle;
        this.tag = tag;
        this.realTimeArrivalTime = realTimeArrivalTime;
        this.realTimeDepartureTime = realTimeDepartureTime;
        this.realTimeCycle = realTimeCycle;
        this.realTimeTag = realTimeTag;
        this.arrivalTimeDeviation = arrivalTimeDeviation;
        this.departureTimeDeviation = departureTimeDeviation;
        this.realTimeTicksUntilArrival = realTimeTicksUntilArrival;
        this.trainState = trainPosition;
    }

    public TrainStop(TrainPrediction prediction) {
        this(prediction.getStationTag(), prediction, false);
    }

    public TrainStop(StationTag tag, TrainPrediction prediction, boolean lastCycle) {
        this(
            prediction.getEntryIndex(), 
            prediction.getSection().getScheduleIndex(),
            prediction.getData().getTrainId(), 
            prediction.getData().getTrain().name.getString(),             
            prediction.getData().getTrain().icon,
            prediction.getData().getTrainInfo(prediction.getEntryIndex()),
            prediction.getTitle(),
            prediction.hasCustomTitle(),
            prediction.getSectionDestinationText(), 
            prediction.getStayDuration(), 
            false,
            lastCycle ? prediction.getPreviousScheduledDepartureTime() : prediction.getScheduledDepartureTime(), 
            lastCycle ? prediction.getPreviousScheduledArrivalTime() : prediction.getScheduledArrivalTime(), 
            prediction.getCurrentCycle() - (lastCycle ? 1 : 0), 
            prediction.getStationTag().getClientTag(prediction.getStationName()), 
            lastCycle ? prediction.getPreviousRealTimeArrivalTime() : prediction.getRealTimeArrivalTime(), 
            lastCycle ? prediction.getPreviousRealTimeDepartureTime() : prediction.getRealTimeDepartureTime(),
            prediction.getCurrentCycle() - (lastCycle ? 1 : 0), 
            prediction.getStationTag().getClientTag(prediction.getStationName()),
            prediction.getArrivalTimeDeviation(), 
            prediction.getDepartureTimeDeviation(), 
            prediction.getRealTimeArrivalTicks(), 
            TrainState.BEFORE
        );
        //updateRealTime(prediction);
    }

    public TrainStop copy() {
        return new TrainStop(
            this.scheduleIndex,
            this.sectionIndex,
            this.trainId,
            this.trainName,
            this.trainIcon,
            this.trainInfo,
            this.scheduleTitle,
            this.isCustomTitle,
            this.terminusText,
            this.stayDuration,
            this.simulated,
            this.scheduledDepartureTime,
            this.scheduledArrivalTime,
            this.cycle,
            this.tag,
            this.realTimeArrivalTime,
            this.realTimeDepartureTime,
            this.realTimeCycle,
            this.realTimeTag,
            this.arrivalTimeDeviation,
            this.departureTimeDeviation,
            this.realTimeTicksUntilArrival,
            this.trainState
        );
    }

    public void simulateTicks(long ticks) {
        this.simulated = true;
        int totalDuration = TrainListener.data.get(getTrainId()).getTotalDuration();
        long scheduledTimeUntilArrival = getScheduledArrivalTime() - DragonLib.getCurrentWorldTime();
        int simulationCycles = (int)(ticks / totalDuration);
        long simulationRemaining = ticks % totalDuration;
        if (simulationRemaining > 0 && simulationRemaining >= scheduledTimeUntilArrival) {
            simulationCycles++;
        }
        
        this.cycle += simulationCycles;
        this.scheduledArrivalTime += simulationCycles * totalDuration;
        this.scheduledDepartureTime += simulationCycles * totalDuration;
        
        this.realTimeArrivalTime += simulationCycles * totalDuration;
        this.realTimeDepartureTime += simulationCycles * totalDuration;
        this.realTimeTicksUntilArrival = -1;
        this.simulationTime += ticks;
    }

    public void simulateCycles(int cycles) {
        if (cycles == 0) {
            return;
        }
        simulateTicks(cycles * TrainListener.data.get(getTrainId()).getTotalDuration());
    }

    
    public static TrainStop simulateCyclesBack(TrainPrediction prediction) {
        return new TrainStop(prediction.getStationTag(), prediction, true);
    }

    public boolean isSimulated() {
        return simulated;
    }

    public long getSimulationTime() {
        return simulationTime;
    }

    public UUID getTrainId() {
        return trainId;
    }    

    public String getTrainName() {
        return trainName;
    }

    public TrainIconType getTrainIcon() {
        return trainIcon;
    }

    public TrainInfo getTrainInfo() {
        return trainInfo;
    }

    public int getScheduleIndex() {
        return scheduleIndex;
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public String getTerminusText() {
        return terminusText;
    }

    public boolean hasCustomTitle() {
        return isCustomTitle;
    }

    public String getScheduleTitle() {
        return scheduleTitle;
    }

    public String getDisplayTitle() {
        return hasCustomTitle() || getTerminusText() == null || getTerminusText().isEmpty() ? getScheduleTitle() : getTerminusText();
    }    

    public String getTrainDisplayName() {
        return getTrainInfo() == null || getTrainInfo().line() == null || getTrainInfo().line().getLineName().isEmpty() ? getTrainName() : getTrainInfo().line().getLineName();
    }

    public int getTrainDisplayColor() {
        if (getTrainInfo() != null && getTrainInfo().line() != null && getTrainInfo().line().getColor() != 0) {
            return getTrainInfo().line().getColor();
        } else if (getTrainInfo() != null && getTrainInfo().group() != null && getTrainInfo().group().getColor() != 0) {
            return getTrainInfo().group().getColor();
        }
        return Constants.COLOR_TRAIN_BACKGROUND;
    }

    public int getStayTime() {
        return stayDuration;
    }

    public StationTag getTag() {
        return GlobalSettings.getInstance().getStationTag(getClientTag().tagId()).orElse(GlobalSettings.getInstance().getOrCreateStationTagFor(TagName.of(getClientTag().tagName())));
    }

    public ClientStationTag getClientTag() {
        return tag;
    }

    public int getScheduledCycle() {
        return cycle;
    }

    public int getRealTimeCycle() {
        return realTimeCycle;
    }

    public ClientStationTag getRealTimeStationTag() {
        return realTimeTag;
    }

    public long getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public long getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public long getRealTimeArrivalTime() {
        return realTimeArrivalTime;
    }

    public long getRealTimeDepartureTime() {
        return realTimeDepartureTime;
    }

    public long getArrivalTimeDeviation() {
        return arrivalTimeDeviation;
    }

    public long getDepartureTimeDeviation() {
        return departureTimeDeviation;
    }

    public int getTicksUntilArrival() {
        return realTimeTicksUntilArrival;
    }    

    public long getScheduledArrivalDay() {
        return getScheduledArrivalTime() / DragonLib.TICKS_PER_DAY;
    }
    
    public long getScheduledDepartureDay() {
        return getScheduledDepartureDay() / DragonLib.TICKS_PER_DAY;
    }
    
    public long getRealTimeArrivalDay() {
        return getRealTimeArrivalTime() / DragonLib.TICKS_PER_DAY;
    }
    
    public long getRealTimeDepartureDay() {
        return getRealTimeDepartureTime() / DragonLib.TICKS_PER_DAY;
    }

    /**
     * The state of this train at this station.
     */
    public TrainState getState() {
        return trainState;
    }
    
    public boolean isArrivalDelayed() {
        return getArrivalTimeDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isDepartureDelayed() {
        return getDepartureTimeDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isAnyDelayed() {
        return isArrivalDelayed() || isDepartureDelayed();
    }

    public boolean shouldRenderRealTime() {
        return getRealTimeCycle() >= getScheduledCycle();
    }

    public boolean isStationInfoChanged() {
        return !getClientTag().info().equals(getRealTimeStationTag().info());
    }

    public boolean isDeparted() {
        return trainState == TrainState.AFTER;
    }

    @Override
    public int compareTo(TrainStop o) {
        return Long.compare(getScheduledArrivalTime(), o.getScheduledArrivalTime());
    }

    public CompoundTag toNbt(boolean includeRealTime) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_SCHEDULE_INDEX, scheduleIndex);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putUUID(NBT_TRAIN_ID, trainId);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        nbt.putString(NBT_TRAIN_ICON, trainIcon.getId().toString());
        nbt.put(NBT_TRAIN_INFO, trainInfo.toNbt());
        nbt.putString(NBT_SCHEDULE_TITLE, scheduleTitle);
        nbt.putString(NBT_TERMINUS_TEXT, terminusText);
        nbt.putInt(NBT_STAY_DURATION, stayDuration);
        nbt.putBoolean(NBT_IS_CUSTOM_TITLE, isCustomTitle);
        nbt.put(NBT_TAG, tag.toNbt());
        nbt.putLong(NBT_SIMULATED_TIME, simulationTime);
        nbt.putLong(NBT_SCHEDULED_ARRIVAL_TIME, scheduledArrivalTime);
        nbt.putLong(NBT_SCHEDULED_DEPARTURE_TIME, scheduledDepartureTime);
        nbt.putInt(NBT_CYCLE, cycle);     
        nbt.putLong(NBT_REAL_TIME_ARRIVAL_TIME, realTimeArrivalTime);
        nbt.putLong(NBT_REAL_TIME_DEPARTURE_TIME, realTimeDepartureTime);
        nbt.putInt(NBT_REAL_CYCLE, realTimeCycle);
        nbt.put(NBT_REAL_TIME_TAG, realTimeTag.toNbt());
        return nbt;
    }

    public static TrainStop fromNbt(CompoundTag nbt) {
        return new TrainStop(
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
}
