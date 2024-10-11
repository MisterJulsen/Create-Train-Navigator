package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.base.Objects;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Data about one single station of a train. */
public class TrainPrediction implements Comparable<TrainPrediction> {

    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_STATION_NAME = "StationName";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_SCHEDULED_TICKS = "ScheduledTicks";
    private static final String NBT_SCHEDULED_REFRESH_TIME = "ScheduledRefreshTime";
    private static final String NBT_REAL_TIME_TICKS = "RealTimeTicks";
    private static final String NBT_REAL_TIME_REFRESH_TIME = "RealTimeRefreshTime";
    private static final String NBT_CURRENT_TICKS_CORRECTION = "CurrentTicksCorrection";
    private static final String NBT_STOPOVERS = "Stopovers";
    private static final String NBT_CYCLE = "Cycle";
    private static final String NBT_CYCLE_TIME = "CycleTime";
    private static final String NBT_STAY_DURATION = "StayDuration";
    private static final String NBT_MIN_STAY_DURATION = "MinStayDuration";

    private transient final TrainData data;
    
    private final int entryIndex;
    private final String title;
    private String stationName;

    private int scheduledTicks;
    private long scheduleRefreshTime;
    private int realTimeTicks;
    private long realTimeRefreshTime;

    private long arrivalTicksCorrection;
    private long departureTicksCorrection;
    private List<String> stopovers = new ArrayList<>();
    private int cycle;
    private long cycleTime;
    private final int stayDuration;
    private final int minStayDuration;

    // History
    private long previousScheduledArrivalTime;
    private long previousScheduledDepartureTime;
    private long previousRealTimeArrivalTime;
    private long previousRealTimeDepartureTime;

    private final Cache<Boolean> isCustomTitle = new Cache<>(() -> {
        if (this.getData().getPredictionsChronologically().isEmpty()) {
            return false;
        }
        TrainPrediction nextPrediction = this.getData().getPredictionsChronologically().get((this.getData().getPredictionsChronologically().indexOf(this) + 1) % this.getData().getPredictionsChronologically().size());
        return !getTitle().matches(nextPrediction.getStationName());
    });
    private final Cache<Boolean> isLastStopOfSection = new Cache<>(() -> {
        TrainTravelSection section = getSection();
        return section.isFinalStop(this);
    });
    private final Cache<TrainTravelSection> section;

    private TrainPrediction(TrainData data, int entryIndex, String stationName, String title, int ticks, int stayDuration, int minStayDuration) {
        this.entryIndex = entryIndex;
        this.data = data;
        this.title = title;
        this.stayDuration = stayDuration;
        this.minStayDuration = minStayDuration;
        this.stationName = stationName;
        this.realTimeTicks = ticks;
        this.realTimeRefreshTime = DragonLib.getCurrentWorldTime();
        this.section = new Cache<>(() -> data.getSectionForIndex(entryIndex));
        
        reset();   
    }

    public TrainPrediction(TrainData data, int entryIndex, TrainDeparturePrediction prediction, int stayDuration, int minStayDuration) {
        this(data, entryIndex, prediction.destination, prediction.scheduleTitle.getString(), prediction.ticks, stayDuration, minStayDuration);
    }

    public static TrainPrediction unpredictable(TrainData data) {
        CreateRailwaysNavigator.LOGGER.warn("Train " + data.getTrain().name.getString() + " (" + data.getTrain().id + ") is unpredictable!");
        return new TrainPrediction(data, -1, "", "", 0, 0, 0);
    }

    /** Resets the scheduled time to the current real time. Called when the total duration changes to prevent deviations. */
    public void reset() {
        this.cycleTime = 0;
        this.departureTicksCorrection = 0;
        this.arrivalTicksCorrection = 0;
        this.scheduledTicks = realTimeTicks;
        this.scheduleRefreshTime = realTimeRefreshTime;        
    }

    /** General data about the train. */
    public TrainData getData() {
        return data;
    }

    /** The index of this entry in the train schedule. */
    public int getEntryIndex() {
        return entryIndex;
    }

    /** The name of the station. */
    public String getStationName() {
        return stationName;
    }

    /** The title, the train has when arriving at this station.  */
    public String getTitle() {
        return title;
    }

    public boolean hasCustomTitle() {
        return isCustomTitle.get();
    }

    /** The scheduled time the train will stay at this station. */
    public int getStayDuration() {
        return stayDuration;
    }

    public int getMinStayDuration() {
        return minStayDuration;
    }

    public List<String> getStopovers() {
        return stopovers;
    }

    /** The world time when the scheduled time was calculated. */
    public long getScheduleRefreshTime() {
        return scheduleRefreshTime;
    }    




    /** The time in ticks of all cycles that have elapsed since the last update. */
    public long getCycleTime() {
        return cycleTime;
    }

    /** The scheduled time until the train stops here. */
    public int getScheduledArrivalTicks() {
        return scheduledTicks;
    }

    /** The scheduled world time when the train arrives at this station. */
    public long getScheduledArrivalTime() {
        return getScheduleRefreshTime() + (long)getScheduledArrivalTicks() + getCycleTime();
    }




    /** The world time when the real time data was last refreshed. */
    public long getRealTimeRefreshTime() {
        return realTimeRefreshTime;
    }

    /** The current time until the train stops here. */
    public int getRealTimeArrivalTicks() {
        return realTimeTicks;
    }

    private long getRealTimeArrivalTimeRaw() {
        return getRealTimeRefreshTime() + getRealTimeArrivalTicks();
    }

    /** The actual deviation from real time and schedule time. Cycles are not taken into account! */
    private long getArrivalTimeRawDeviation() {
        return getRealTimeArrivalTimeRaw() - getScheduledArrivalTime();
    }


    /** The current world time the train will arrive at this station. */
    public long getRealTimeArrivalTime() {
        return getRealTimeArrivalTimeRaw() - arrivalTicksCorrection;
    }

    /** The actual deviation from real time and schedule time. */
    public long getArrivalTimeDeviation() {
        return getArrivalTimeRawDeviation() - arrivalTicksCorrection;
    }












    /** The departure time from this stop when the schedule was updated. */
    public int getScheduledDepartureTicks() {
        return getScheduledArrivalTicks() + getStayDuration();
    }

    /** The scheduled world time when the train departs from this station. */
    public long getScheduledDepartureTime() {
        return getScheduledArrivalTime() + getStayDuration();
    }

    /** The current world time at which the train will depart. */
    public long getRealTimeDepartureTime() {
        return getRealTimeArrivalTime() + getStayDuration() - departureTicksCorrection;// - Math.min(getBufferTime(), getDeviationArrivalTime());
    }

    /** The deviation of the departure time from the schedule. */
    public long getDepartureTimeDeviation() {
        return getArrivalTimeDeviation() - departureTicksCorrection;
    }




    /** The time it took the train to get here from the last station. */
    public int getLastTransitTime() {
        return data.getTransitTicks();
    }

    public long getBufferTime() {
        return Math.max(getStayDuration() - getMinStayDuration(), 0);
    }

    /** The remaining buffer time that the train can use to catch up for delays. */
    public long getBufferTimeLeft() {
        return getBufferTime() - data.waitingAtStationTicks();
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

    public void setStopovers(List<String> stopovers) {
        this.stopovers = stopovers;
    }


    /** Change this stop to the next cycle. */
    public void nextCycle() {
        this.previousScheduledArrivalTime = getScheduledArrivalTime();
        this.previousScheduledDepartureTime = getScheduledDepartureTime();
        this.previousRealTimeArrivalTime = getRealTimeArrivalTime();
        this.previousRealTimeDepartureTime = getRealTimeDepartureTime();

        cycle++;
        this.cycleTime += data.getTotalDuration();
    }

    /** The cycle the train is currently in. */
    public int getCurrentCycle() {
        return cycle;
    }

    
    public long getPreviousScheduledArrivalTime() {
        return previousScheduledArrivalTime;
    }

    public long getPreviousScheduledDepartureTime() {
        return previousScheduledDepartureTime;
    }

    public long getPreviousRealTimeArrivalTime() {
        return previousRealTimeArrivalTime;
    }

    public long getPreviousRealTimeDepartureTime() {
        return previousRealTimeDepartureTime;
    }

    

    /** Calculates in which cycle the train will be when it arrives back here in the specified time.*/    
    public int estimateCycleIn(int ticks) {
        return getCurrentCycle() + ticks / data.getTotalDuration();
    }

    /** Time since start of recording. */
    public long getRuntime() {
        return DragonLib.getCurrentWorldTime() - getScheduleRefreshTime();
    }

    public boolean hasDepartedOnce() {
        return getCurrentCycle() > 0;
    }

    public boolean isArrivalDelayed() {
        return getRealTimeArrivalTime() - ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get() > getScheduledArrivalTime();
    }

    public boolean isDepartureDelayed() {
        return getRealTimeDepartureTime() - ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get() > getScheduledDepartureTime();
    }

    public boolean isAnyDelayed() {
        return isArrivalDelayed() || isDepartureDelayed();
    }

    /**
     * Get the station tag for this station. Server-side only!
     * @return The StationTag for this stop.
     * @throws RuntimeSideException Thrown when called on the wrong logical side.
     */
    public StationTag getStationTag() throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return GlobalSettings.getInstance().getOrCreateStationTagFor(stationName);
    }

    public TrainTravelSection getSection() {
        TrainTravelSection sec = section.get();
        if (sec.isDefault()) {
            section.clear();
        }
        return sec;
    }    

    public String getSectionDestinationText() {
        TrainTravelSection sec = section.get();
        if (sec.isDefault()) {
            section.clear();
        }
        return isLastStopOfSection.get() ? sec.nextSection().getDisplayText() : sec.getDisplayText();
    }

    

    public void updateRealTime(String stationName, int realTimeTicks) {
        isCustomTitle.clear();
        this.stationName = stationName == null ? this.stationName : stationName;
        this.realTimeRefreshTime = DragonLib.getCurrentWorldTime();
        this.realTimeTicks = realTimeTicks;

        List<TrainPrediction> prevPreds = data.getPredictionsChronologically();
        Optional<TrainPrediction> currentPrediction = data.getNextStopPrediction();
        this.arrivalTicksCorrection = 0;
        this.departureTicksCorrection = 0;

        if (data.isWaitingAtStation() && data.getCurrentScheduleIndex() == getEntryIndex()) {
            this.arrivalTicksCorrection = Math.min(data.waitingAtStationTicks(), getStayDuration());
            //this.arrivalTicksCorrection = data.waitingAtStationTicks();
        }

        if (currentPrediction.isPresent()) {
            this.arrivalTicksCorrection = currentPrediction.get().arrivalTicksCorrection;
        }
        
        long tempDepartureCorrection = getBufferTime();
        long tempArrivalCorrection = 0;//getBufferTime();
        for (int i = 0; i < prevPreds.size(); i++) {
            final TrainPrediction pred = prevPreds.get(i);
            //tempArrivalCorrection += getBufferTime();
            tempArrivalCorrection += pred.getBufferTime();
            if (pred == this) break;
        }
        this.arrivalTicksCorrection += Math.min(tempArrivalCorrection, getArrivalTimeDeviation());
        this.departureTicksCorrection += Math.min(tempDepartureCorrection, getArrivalTimeDeviation());
    }

    @Override
    public boolean equals(Object obj) {
        return 
            obj instanceof TrainPrediction o &&
            scheduledTicks == o.scheduledTicks &&
            scheduleRefreshTime == o.scheduleRefreshTime &&
            entryIndex == o.entryIndex &&
            stationName.equals(o.stationName)
        ;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(scheduledTicks, scheduleRefreshTime, entryIndex, stationName);
    }

    public boolean similarTo(Object obj) {
        return 
            obj instanceof TrainPrediction o &&
            stationName.equals(o.stationName) &&
            entryIndex == o.entryIndex
        ;
    }

    @Override
    public String toString() {
        return formattedText().getString();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag stopovers = new ListTag();
        stopovers.addAll(this.stopovers.stream().map(x -> StringTag.valueOf(x)).toList());

        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putString(NBT_STATION_NAME, stationName == null ? "" : stationName);
        nbt.putString(NBT_TITLE, title == null ? "" : title);
        nbt.putInt(NBT_SCHEDULED_TICKS, scheduledTicks);
        nbt.putLong(NBT_SCHEDULED_REFRESH_TIME, scheduleRefreshTime);
        nbt.putInt(NBT_REAL_TIME_TICKS, realTimeTicks);
        nbt.putLong(NBT_REAL_TIME_REFRESH_TIME, realTimeRefreshTime);
        nbt.putLong(NBT_CURRENT_TICKS_CORRECTION, departureTicksCorrection);
        nbt.put(NBT_STOPOVERS, stopovers);
        nbt.putInt(NBT_CYCLE, cycle);
        nbt.putLong(NBT_CYCLE_TIME, cycleTime);
        nbt.putInt(NBT_STAY_DURATION, stayDuration);
        nbt.putInt(NBT_MIN_STAY_DURATION, minStayDuration);
        return nbt;
    }

    public static TrainPrediction fromNbt(TrainData data, CompoundTag nbt) {
        TrainPrediction pred = new TrainPrediction(
            data,
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getString(NBT_STATION_NAME),
            nbt.getString(NBT_TITLE),
            nbt.getInt(NBT_SCHEDULED_TICKS),
            nbt.getInt(NBT_STAY_DURATION),
            nbt.getInt(NBT_MIN_STAY_DURATION)
        );
        pred.deserializeNbt(nbt);
        return pred;
    }

    protected void deserializeNbt(CompoundTag nbt) {
        this.scheduledTicks = nbt.getInt(NBT_SCHEDULED_TICKS);
        this.scheduleRefreshTime = nbt.getLong(NBT_SCHEDULED_REFRESH_TIME);
        this.realTimeTicks = nbt.getInt(NBT_REAL_TIME_TICKS);
        this.realTimeRefreshTime = nbt.getLong(NBT_REAL_TIME_REFRESH_TIME);
        this.departureTicksCorrection = nbt.getLong(NBT_CURRENT_TICKS_CORRECTION);
        this.stopovers = new ArrayList<>(nbt.getList(NBT_STOPOVERS, Tag.TAG_STRING).stream().map(x -> x.getAsString()).toList());
        this.cycle = nbt.getInt(NBT_CYCLE);
        this.cycleTime = nbt.getLong(NBT_CYCLE_TIME);
    }

    /**
     * DEBUG ONLY!
     */
    public Component formattedText() {
        return TextUtils.text("[ " + entryIndex + " ]: ").withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(stationName).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("*" + getCurrentCycle()).withStyle(ChatFormatting.YELLOW))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("CT: " + getRealTimeArrivalTime()).withStyle(ChatFormatting.GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("D: " + (getArrivalTimeDeviation() + " / " + getDepartureTimeDeviation())).withStyle(ChatFormatting.GOLD))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("B: " + (departureTicksCorrection)).withStyle(ChatFormatting.DARK_GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("P: " + (getScheduledArrivalTime())).withStyle(ChatFormatting.BLUE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("S: " + getStayDuration()).withStyle(ChatFormatting.AQUA))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("Tr: " + getLastTransitTime()).withStyle(ChatFormatting.DARK_RED))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("T: " + getSection()).withStyle(ChatFormatting.RED))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("Ti: " + title).withStyle(ChatFormatting.LIGHT_PURPLE))
        ;
    }

    public void shiftTime(long l) {
        this.scheduleRefreshTime += l;
        this.realTimeRefreshTime += l;
    }

    @Override
    public int compareTo(TrainPrediction o) {
        return Long.compare(getScheduledArrivalTime(), o.getScheduledArrivalTime());
    }
}
