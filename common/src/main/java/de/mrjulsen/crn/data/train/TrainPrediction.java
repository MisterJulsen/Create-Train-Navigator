package de.mrjulsen.crn.data.train;

import java.util.List;

import com.google.common.base.Objects;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.PredictionTimes.DepartureTime;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.util.PrimaryStringSelector;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.ITimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Data about one single station of a train. */
public class TrainPrediction implements Comparable<TrainPrediction> {    

    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_STATION_FILTER = "StationFilter";
    private static final String NBT_STATION_NAME = "StationName";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_CYCLE = "Cycle";
    private static final String NBT_TRANSIT_TIME = "TransitTime";
    private static final String NBT_SCHEDULED_TIMES = "ScheduledTimes";
    private static final String NBT_REAL_TIMES = "RealTimes";
    private static final String NBT_AVERAGE_STAY_DURATION = "AverageStayDuration";

    private transient final TrainData data;
    
    private final int entryIndex;
    private String title;
    private String stationFilter;
    private String stationName;
    private final PrimaryStringSelector recentStationNames = new PrimaryStringSelector(10);


    // TIMES
    private PredictionTimes scheduledTimes = new PredictionTimes(this, 0, 0, 0, 0);;
    private PredictionTimes realTimes = new PredictionTimes(this, 0, 0, 0, 0);
    private int averageStayDuration = -1;
    private int cycle;

    // Transit times
    private final ValueWatcher transitTime = new ValueWatcher(ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get(), ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get() * 2 + 1, () -> getData().updateTotalDuration());

    // History
    private long previousScheduledArrivalTime;
    private long previousScheduledDepartureTime;
    private long previousRealTimeArrivalTime;
    private long previousRealTimeDepartureTime;

    // Flags
    private boolean shouldSoftReset;

    private final Cache<Boolean> isCustomTitle = new Cache<>(() -> {
        if (getTitle() == null || getTitle().isEmpty()) {
            return false;
        }
        if (this.getData().getPredictionsChronologically().isEmpty()) {
            return false;
        }
        TrainPrediction nextPrediction = this.getData().getPredictionsChronologically().get((this.getData().getPredictionsChronologically().indexOf(this) + 1) % this.getData().getPredictionsChronologically().size());
        return !getTitle().matches(nextPrediction.getTargetedStationName());
    });
    private final Cache<Boolean> isLastStopOfSection = new Cache<>(() -> {
        ScheduleSection section = getSection();
        return section.isFinalStop(this);
    });
    private final Cache<StationTag> tagCache = new Cache<>(() -> GlobalSettings.getInstance().getOrCreateStationTagFor(getTargetedStationName()));
    private final Cache<StationTag> estimatedTagCache = new Cache<>(() -> GlobalSettings.getInstance().getOrCreateStationTagFor(getScheduledStationName()));
    private final Cache<ScheduleSection> section;

    public TrainPrediction(TrainData data, int entryIndex, String stationFilter, String stationName, String title) {
        this.entryIndex = entryIndex;
        this.data = data;
        this.stationFilter = stationFilter;

		int size = data.getTrain().runtime.getSchedule().entries.size();
        String text = title;
		if (text.isBlank()) {
			for (int i = 1; i < size; i++) {
				int j = (entryIndex + i) % size;
				ScheduleEntry scheduleEntry = data.getTrain().runtime.getSchedule().entries.get(j);
				if (!(scheduleEntry.instruction instanceof DestinationInstruction instruction))
					continue;
				text = instruction.getFilter()
					.replaceAll("\\*", "")
					.trim();
				break;
			}
		}
        this.title = text;

        this.stationName = stationName;
        this.section = new Cache<>(() -> data.getSectionForIndex(entryIndex));
    }

    private boolean preInitialized = false;
    public void preInit() {
        if (preInitialized || isInitialized()) {
            return;
        }

        List<Integer> createTransitTimes = ((ScheduleRuntimeAccessor)data.getTrain().runtime).crn$getTransitTicks();
        int createTransitTime = entryIndex < createTransitTimes.size() ? createTransitTimes.get(entryIndex) : -1;
        if (createTransitTime >= 0 && !data.isPreInitializationPhase()) {
            this.transitTime().add(createTransitTime, false);
        }
        preInitialized = true;
    }

    public static TrainPrediction unpredictable(TrainData data) {
        CreateRailwaysNavigator.LOGGER.warn("Train " + data.getTrain().name.getString() + " (" + data.getTrain().id + ") is unpredictable!");
        return new TrainPrediction(data, -1, "", "", "");
    }

    /**
     * Resets the scheduled times to the current real time. Always called when the total duration changes to prevent deviations.
     */
    private void reset() {
        this.scheduledTimes = this.realTimes;
        this.shouldSoftReset = false;
    }

    public void queueReset() {
        this.shouldSoftReset = true;
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
    public String getTargetedStationName() {
        return stationName;
    }

    public String getStationFilter() {
        return stationFilter;
    }

    private String getEstimatedStationName() {
        return recentStationNames.getCurrentPrimary();
    }

    public String getScheduledStationName() {
        if (TrainUtils.stationExists(getStationFilter())) {
            return getStationFilter();
        } else if (getEstimatedStationName() != null && TrainUtils.stationExists(getEstimatedStationName())) {
            return getEstimatedStationName();
        }
        return getTargetedStationName();
    }

    public String getRealTimeStationName() {
        if (TrainUtils.stationExists(getTargetedStationName()) || getEstimatedStationName() == null) {
            return getTargetedStationName();
        }
        return getEstimatedStationName();
    }

    public StationTag getEstimatedStationTag() throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return estimatedTagCache.get();
    }

    /** The title, the train has when arriving at this station.  */
    public String getTitle() {
        return title;
    }

    public boolean hasCustomTitle() {
        return isCustomTitle.get();
    }

    public int getAverageStayDuration() {
        return averageStayDuration <= 0 ? (int)scheduled().stayDuration() : averageStayDuration;
    }

    public ValueWatcher transitTime() {
        return this.transitTime;
    }
    

    public PredictionTimes scheduled() {
        return scheduledTimes;
    }

    public PredictionTimes realTime() {
        return realTimes;
    }
    


    // TIME DEVIATION

    /** The actual deviation from real time and schedule time. */
    public long getArrivalTimeDeviation() {
        return realTime().arrivalTime() - scheduled().arrivalTime();
    }

    /** The deviation of the departure time from the schedule. */
    public long getDepartureTimeDeviation() {
        return realTime().departureTime() - scheduled().departureTime();
    }



    public long getBufferTime() {
        return Math.max(scheduled().stayDuration() - scheduled().minStayDuration(), 0);
    }

    /** The remaining buffer time that the train can use to catch up for delays. */
    public long getBufferTimeLeft() {
        return getBufferTime() - data.waitingAtStationTicks();
    }

    public long getScheduledArrivalDay() {
        ITimeSystem system = new ConfiguredTimeSystem();
        return scheduled().arrivalTime() / system.getTicksPerDay();
    }
    
    public long getScheduledDepartureDay() {
        ITimeSystem system = new ConfiguredTimeSystem();
        return scheduled().departureTime() / system.getTicksPerDay();
    }
    
    public long getRealTimeArrivalDay() {
        ITimeSystem system = new ConfiguredTimeSystem();
        return realTime().arrivalTime() / system.getTicksPerDay();
    }
    
    public long getRealTimeDepartureDay() {
        ITimeSystem system = new ConfiguredTimeSystem();
        return realTime().departureTime() / system.getTicksPerDay();
    }


    /** Change this stop to the next cycle. */
    public void nextCycle() {
        this.previousScheduledArrivalTime = scheduled().arrivalTime();
        this.previousScheduledDepartureTime = scheduled().departureTime();
        this.previousRealTimeArrivalTime = realTime().arrivalTime();
        this.previousRealTimeDepartureTime = realTime().departureTime();

        this.cycle++;
        this.scheduled().shift(data.getTotalDuration(), false);
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

    

    void updateAverageStayDuration(int value) {
        int oldAverageStayDuration = this.averageStayDuration;
        if (this.averageStayDuration < 0) {
            this.averageStayDuration = value; 
        } else {
            this.averageStayDuration = (this.averageStayDuration + value) / 2;
        }

        if (Math.abs(oldAverageStayDuration - this.averageStayDuration) > ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get()) {
            //this.getData().updateTotalDuration();
        }
    }

    /** Calculates in which cycle the train will be when it arrives back here in the specified time.*/    
    public int estimateCycleIn(int ticks) {
        return getCurrentCycle() + ticks / data.getTotalDuration();
    }

    /** Time since start of recording. */
    public long getRuntime() {
        return DragonLib.getCurrentWorldTime() - scheduled().refreshTime();
    }

    public boolean hasDepartedOnce() {
        return getCurrentCycle() > 0;
    }

    public boolean isArrivalDelayed() {
        return realTime().arrivalTime() - ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get() > scheduled().arrivalTime();
    }

    public boolean isDepartureDelayed() {
        return realTime().departureTime() - ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get() > scheduled().departureTime();
    }

    public boolean isAnyDelayed() {
        return isArrivalDelayed() || isDepartureDelayed();
    }

    public boolean isInitialized() {
        return scheduled() != null && transitTime().isInitialized();
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
        return tagCache.get();
    }

    public ScheduleSection getSection() {
        ScheduleSection sec = section.get();
        if (sec.isDefault()) {
            section.clear();
        }
        return sec;
    }    

    public String getSectionDestinationText() {
        ScheduleSection sec = section.get();
        if (sec.isDefault()) {
            section.clear();
        }
        return isLastStopOfSection.get() && !sec.shouldIncludeNextStationOfNextSection() ? sec.nextSection().getDisplayText() : sec.getDisplayText();
    }

    

    public void updateRealTime(String stationFilter, String stationName, long refreshTime, long arrivalTime, String title) {
        isCustomTitle.clear();
        this.stationFilter = stationFilter == null ? this.stationFilter : stationFilter;
        this.stationName = stationName == null ? this.stationName : stationName;
        this.title = title;
        
        DepartureTime departures = estimateDepartures(getData().getTrain(), entryIndex, arrivalTime);
        this.realTimes = new PredictionTimes(this, refreshTime, arrivalTime, departures.defaultDepartureTime(), departures.minDepartureTime());
        if (scheduled() == null || this.shouldSoftReset) {
            reset();
        }
        resetAllTimedCaches();
    }

    private void resetAllTimedCaches() {
        tagCache.clear();
        estimatedTagCache.clear();
    }

    public void onReachStation() {
        recentStationNames.addString(stationName);
    }

    public static DepartureTime estimateDepartures(Train train, int entryIndex, long triggerTime) {
		ScheduleEntry scheduleEntry = train.runtime.getSchedule().entries.get(entryIndex);

        long[] currentTime = new long[] { triggerTime, triggerTime };
		for (List<ScheduleWaitCondition> list : scheduleEntry.conditions) {
			for (ScheduleWaitCondition condition : list) {
                if (condition instanceof IPredictableWaitCondition c) {
                    currentTime[0] = c.waitUntil(currentTime[0]);
                    currentTime[1] = c.waitMinUntil(currentTime[1]);
                }
			}
		}

        return new DepartureTime(currentTime[0], currentTime[1]);
	}








    @Override
    public boolean equals(Object obj) {
        return 
            obj instanceof TrainPrediction o &&
            scheduled().equals(o.scheduled()) &&
            entryIndex == o.entryIndex &&
            stationName.equals(o.stationName)
        ;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(scheduled(), realTime(), entryIndex, stationName);
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

        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putString(NBT_STATION_FILTER, stationFilter == null ? "" : stationFilter);
        nbt.putString(NBT_STATION_NAME, stationName == null ? "" : stationName);
        nbt.putString(NBT_TITLE, title == null ? "" : title);
        if (scheduled() != null) nbt.put(NBT_SCHEDULED_TIMES, scheduled().toNbt());
        if (realTime() != null) nbt.put(NBT_REAL_TIMES, realTime().toNbt());
        nbt.putInt(NBT_CYCLE, cycle);
        nbt.putInt(NBT_TRANSIT_TIME, transitTime().value());
        nbt.putInt(NBT_AVERAGE_STAY_DURATION, getAverageStayDuration());
        return nbt;
    }

    public static TrainPrediction fromNbt(TrainData data, CompoundTag nbt) {
        TrainPrediction pred = new TrainPrediction(
            data,
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getString(NBT_STATION_FILTER),
            nbt.getString(NBT_STATION_NAME),
            nbt.getString(NBT_TITLE)
        );
        pred.deserializeNbt(nbt);
        return pred;
    }

    protected void deserializeNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_SCHEDULED_TIMES)) this.scheduledTimes = PredictionTimes.fromNbt(this, nbt.getCompound(NBT_SCHEDULED_TIMES));
        if (nbt.contains(NBT_REAL_TIMES)) this.realTimes = PredictionTimes.fromNbt(this, nbt.getCompound(NBT_REAL_TIMES));
        this.cycle = nbt.getInt(NBT_CYCLE);
        this.transitTime.forceValue(nbt.getInt(NBT_TRANSIT_TIME));
        this.averageStayDuration = nbt.getInt(NBT_AVERAGE_STAY_DURATION);
    }

    /**
     * DEBUG ONLY!
     */
    public Component formattedText() {
        return TextUtils.text("[ " + entryIndex + " ]: ").withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(getTargetedStationName()).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("*" + getCurrentCycle()).withStyle(ChatFormatting.YELLOW))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("sA: " + (scheduled().arrivalTime())).withStyle(ChatFormatting.BLUE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("rA: " + realTime().arrivalTime()).withStyle(ChatFormatting.GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("d: " + (getArrivalTimeDeviation() + " / " + getDepartureTimeDeviation())).withStyle(ChatFormatting.GOLD))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("B: " + (getBufferTime())).withStyle(ChatFormatting.DARK_GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("W: " + scheduled().stayDuration() + " / " + scheduled().minStayDuration()).withStyle(ChatFormatting.AQUA))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("S: " + getSection()).withStyle(ChatFormatting.RED))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("T: " + title).withStyle(ChatFormatting.LIGHT_PURPLE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("Z: " + getStationFilter() + " / " + getTargetedStationName() + " / " + getEstimatedStationName()).withStyle(ChatFormatting.DARK_PURPLE))
        ;
    }

    public void shiftTime(long l) {
        DLUtils.doIfNotNull(this.scheduled(), x -> x.shift(l, true));
        DLUtils.doIfNotNull(this.realTime(), x -> x.shift(l, true));
    }

    @Override
    public int compareTo(TrainPrediction o) {
        return Long.compare(scheduled().arrivalTime(), o.scheduled().arrivalTime());
    }
}
