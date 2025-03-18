package de.mrjulsen.crn.data.train;

import java.util.List;
import java.util.Optional;

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
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Data about one single station of a train. */
public class TrainPrediction implements Comparable<TrainPrediction> {

    public static record DepartureTime(long defaultDepartureTime, long minDepartureTime) {}

    public static class PredictionTimes {
        private static final String NBT_REFRESHED = "Refreshed";
        private static final String NBT_ARRIVAL = "Arrival";
        private static final String NBT_DEPARTURE = "Departure";
        private static final String NBT_MIN_DEPARTURE = "MinDeparture";

        private final TrainPrediction prediction;

        private long refreshTime;
        private long arrivalTime;
        private long departureTime;
        private long minDepartureTime;

        public PredictionTimes(TrainPrediction prediction, long refreshTime, long arrivalTime, long departureTime, long minDepartureTime) {
            this.prediction = prediction;
            this.set(refreshTime, arrivalTime, departureTime, minDepartureTime);
        }

        void set(long refreshTime, long arrivalTime, long departureTime, long minDepartureTime) {
            this.refreshTime = refreshTime;
            this.arrivalTime = Math.max(this.refreshTime, arrivalTime);
            this.departureTime = Math.max(this.arrivalTime, departureTime);
            this.minDepartureTime = Math.max(this.arrivalTime, minDepartureTime);
        }

        void shift(long amount, boolean updateRefreshTime) {            
            if (updateRefreshTime) {
                this.refreshTime += amount;
            }
            this.arrivalTime += amount;
            DepartureTime dt = estimateDepartures(prediction.getData().getTrain(), prediction.entryIndex, arrivalTime);
            this.departureTime = dt.defaultDepartureTime();
            this.minDepartureTime = dt.minDepartureTime();
        }

        public long refreshTime() {
            return refreshTime;
        }

        public long arrivalTime() {
            return arrivalTime;
        }

        public long departureTime() {
            return departureTime;
        }

        public long minDepartureTime() {
            return minDepartureTime;
        }

        public long arrivalIn() {
            return arrivalTime() - refreshTime();
        }

        public long departureIn() {
            return departureTime() - refreshTime();
        }

        public long minDepartureIn() {
            return minDepartureTime() - refreshTime();
        }

        public long stayDuration() {
            return Math.max(0, departureTime() - arrivalTime());
        }

        public long minStayDuration() {
            return Math.max(0, minDepartureTime() - arrivalTime());
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong(NBT_REFRESHED, refreshTime);
            nbt.putLong(NBT_ARRIVAL, arrivalTime);
            nbt.putLong(NBT_DEPARTURE, departureTime);
            nbt.putLong(NBT_MIN_DEPARTURE, minDepartureTime);
            return nbt;
        }

        public static PredictionTimes fromNbt(TrainPrediction prediction, CompoundTag nbt) {
            long refreshTime = nbt.getLong(NBT_REFRESHED);
            long arrivalTime = nbt.getLong(NBT_ARRIVAL);
            long departureTime = nbt.getLong(NBT_DEPARTURE);
            long minDepartureTime = nbt.getLong(NBT_MIN_DEPARTURE);
            PredictionTimes t = new PredictionTimes(prediction, refreshTime, arrivalTime, departureTime, minDepartureTime);
            return t;
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof PredictionTimes o) {
                return refreshTime() == o.refreshTime() && arrivalTime() == o.arrivalTime();
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return 31 * Objects.hashCode(refreshTime(), arrivalTime());
        }

        @Override
        public final String toString() {
            return String.format("PredictionTimes[R: %s, A: %s, D: %s, At: %s, Dt: %s, d: %s]", refreshTime(), arrivalTime(), departureTime(), arrivalIn(), departureIn(), stayDuration());
        }
    }

    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_STATION_NAME = "StationName";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_CURRENT_TICKS_CORRECTION = "CurrentTicksCorrection";
    private static final String NBT_CYCLE = "Cycle";
    private static final String NBT_TRANSIT_TIME = "TransitTime";
    private static final String NBT_SCHEDULED_TIMES = "ScheduledTimes";
    private static final String NBT_REAL_TIMES = "RealTimes";
    private static final String NBT_AVERAGE_STAY_DURATION = "AverageStayDuration";

    private transient final TrainData data;
    
    private final int entryIndex;
    private final String title;
    private String stationName;


    // TIMES
    private PredictionTimes scheduledTimes;
    private PredictionTimes realTimes;
    private int averageStayDuration = -1;

    private long waitAtStationBufferTicks;
    private long availableDepartureBufferTime;
    private int cycle;

    // Transit times
    private final ValueWatcher transitTime = new ValueWatcher(ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get(), ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get() * 2 + 1, () -> getData().updateTotalDuration());


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
    private final Cache<StationTag> tagCache = new Cache<>(() -> GlobalSettings.getInstance().getOrCreateStationTagFor(stationName));
    private final Cache<TrainTravelSection> section;

    public TrainPrediction(TrainData data, int entryIndex, String stationName, String title) {
        this.entryIndex = entryIndex;
        this.data = data;

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
        return new TrainPrediction(data, -1, "", "");
    }

    /**
     * Resets the scheduled times to the current real time. Always called when the total duration changes to prevent deviations.
     */
    public void reset() {
        this.availableDepartureBufferTime = 0;
        this.waitAtStationBufferTicks = 0;
        this.scheduledTimes = this.realTimes;
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

    public int getAverageStayDuration() {
        return averageStayDuration < 0 ? (int)scheduled().stayDuration() : averageStayDuration;
    }

    /** The scheduled time the train will stay at this station. */
    @Deprecated
    public int getStayDuration() {
        return (int)scheduled().stayDuration();
    }

    /** The minimum time the train will stay at this station. */
    @Deprecated
    public int getMinStayDuration() {
        return (int)scheduled().minStayDuration();
    }

    /** The current transit time, which the train needed to get here from the last station. */
    public int getTransitTime() {
        return this.transitTime.value();
    }

    public int getLastMeasuredTransitTime() {
        return this.transitTime.measuredValue();
    }

    public Integer[] getTransitTimesHistory() {
        return this.transitTime.history();
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


    // ##### ARRIVAL #####
    // SCHEDULED TIMES

    /** The world time when the scheduled time was calculated. */
    @Deprecated
    public long getScheduledWorldTime() {
        return scheduled().refreshTime();
    }

    /** The scheduled time until the train stops here. */
    @Deprecated
    public int getScheduledArrivalTicks() {
        return (int)scheduled().arrivalIn();
    }

    /** The scheduled world time when the train arrives at this station. */
    @Deprecated
    public long getScheduledArrivalTime() {
        return scheduled().arrivalTime();
    }


    // REAL TIME

    /** The world time when the real time data was last refreshed. */
    @Deprecated
    public long getRealTimeWorldTime() {
        return realTime().refreshTime();
    }

    /** The current time until the train stops here. */
    @Deprecated
    public int getRealTimeArrivalTicks() {
        return (int)realTime().arrivalIn();
    }

    /** The current world time the train will arrive at this station. */
    public long getRealTimeArrivalTime() {
        return realTime().arrivalTime();// - waitAtStationBufferTicks;
    }


    // TIME DEVIATION

    /** The actual deviation from real time and schedule time. Cycles are not taken into account! */
    private long getArrivalTimeDeviationRaw() {
        return realTime().arrivalTime() - scheduled().arrivalTime();
    }

    /** The actual deviation from real time and schedule time. */
    public long getArrivalTimeDeviation() {
        return getArrivalTimeDeviationRaw();// - waitAtStationBufferTicks;
    }





    // ##### DEPARTURE #####

    /** The departure time from this stop when the schedule was updated. */
    @Deprecated
    public int getScheduledDepartureTicks() {
        return (int)scheduled().departureIn();
    }

    /** The scheduled world time when the train departs from this station. */
    @Deprecated
    public long getScheduledDepartureTime() {
        return scheduled().departureTime();
    }

    /** The current world time at which the train will depart. */
    public long getRealTimeDepartureTime() {
        return realTime().departureTime();// - availableDepartureBufferTime;
    }

    /** The deviation of the departure time from the schedule. */
    public long getDepartureTimeDeviation() {
        return realTime().departureTime() - scheduled().departureTime();// - availableDepartureBufferTime;
    }



    public long getBufferTime() {
        return Math.max(getStayDuration() - getMinStayDuration(), 0);
    }

    /** The remaining buffer time that the train can use to catch up for delays. */
    public long getBufferTimeLeft() {
        return getBufferTime() - data.waitingAtStationTicks();
    }

    public long getScheduledArrivalDay() {
        return scheduled().arrivalTime() / DragonLib.ticksPerDay();
    }
    
    public long getScheduledDepartureDay() {
        return getScheduledDepartureDay() / DragonLib.ticksPerDay();
    }
    
    public long getRealTimeArrivalDay() {
        return realTime().arrivalTime() / DragonLib.ticksPerDay();
    }
    
    public long getRealTimeDepartureDay() {
        return getRealTimeDepartureTime() / DragonLib.ticksPerDay();
    }


    /** Change this stop to the next cycle. */
    public void nextCycle() {
        this.previousScheduledArrivalTime = getScheduledArrivalTime();
        this.previousScheduledDepartureTime = getScheduledDepartureTime();
        this.previousRealTimeArrivalTime = getRealTimeArrivalTime();
        this.previousRealTimeDepartureTime = getRealTimeDepartureTime();

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
        if (this.averageStayDuration < 0) {
            this.averageStayDuration = value; 
        } else {
            this.averageStayDuration = (this.averageStayDuration + value) / 2;
        }
    }

    /** Calculates in which cycle the train will be when it arrives back here in the specified time.*/    
    public int estimateCycleIn(int ticks) {
        return getCurrentCycle() + ticks / data.getTotalDuration();
    }

    /** Time since start of recording. */
    public long getRuntime() {
        return DragonLib.getCurrentWorldTime() - getScheduledWorldTime();
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

    

    public void updateRealTime(String stationName, long refreshTime, long triggerTime) {
        isCustomTitle.clear();
        this.stationName = stationName == null ? this.stationName : stationName;
        
        DepartureTime departures = estimateDepartures(getData().getTrain(), entryIndex, triggerTime);
        this.realTimes = new PredictionTimes(this, refreshTime, triggerTime, departures.defaultDepartureTime(), departures.minDepartureTime());
        if (scheduled() == null) {
            reset();
        }

        List<TrainPrediction> prevPreds = data.getPredictionsChronologically();
        Optional<TrainPrediction> currentPrediction = data.getNextStopPrediction();
        this.waitAtStationBufferTicks = 0;
        this.availableDepartureBufferTime = 0;

        if (data.isAtStation() && data.getCurrentScheduleIndex() == getEntryIndex()) {
            this.waitAtStationBufferTicks = Math.min(data.waitingAtStationTicks(), getStayDuration());
        }

        if (currentPrediction.isPresent()) {
            this.waitAtStationBufferTicks = currentPrediction.get().waitAtStationBufferTicks;
        }
        
        long tempDepartureBufferTime = getBufferTime();
        long tempWaitAtStationBufferTicks = 0;
        for (int i = 0; i < prevPreds.size(); i++) {
            final TrainPrediction pred = prevPreds.get(i);
            tempWaitAtStationBufferTicks += pred.getBufferTime();
            if (pred == this) break;
        }
        this.waitAtStationBufferTicks += Math.min(tempWaitAtStationBufferTicks, getArrivalTimeDeviation() /* delay */);
        this.availableDepartureBufferTime += Math.min(tempDepartureBufferTime, getArrivalTimeDeviation() /* delay */);
        resetAllTimedCaches();
    }

    private void resetAllTimedCaches() {
        tagCache.clear();
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
        nbt.putString(NBT_STATION_NAME, stationName == null ? "" : stationName);
        nbt.putString(NBT_TITLE, title == null ? "" : title);
        nbt.put(NBT_SCHEDULED_TIMES, scheduled().toNbt());
        nbt.put(NBT_REAL_TIMES, realTime().toNbt());
        nbt.putLong(NBT_CURRENT_TICKS_CORRECTION, availableDepartureBufferTime);
        nbt.putInt(NBT_CYCLE, cycle);
        nbt.putInt(NBT_TRANSIT_TIME, getTransitTime());
        nbt.putInt(NBT_AVERAGE_STAY_DURATION, getAverageStayDuration());
        return nbt;
    }

    public static TrainPrediction fromNbt(TrainData data, CompoundTag nbt) {
        TrainPrediction pred = new TrainPrediction(
            data,
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getString(NBT_STATION_NAME),
            nbt.getString(NBT_TITLE)
        );
        pred.deserializeNbt(nbt);
        return pred;
    }

    protected void deserializeNbt(CompoundTag nbt) {
        this.scheduledTimes = PredictionTimes.fromNbt(this, nbt.getCompound(NBT_SCHEDULED_TIMES));
        this.realTimes = PredictionTimes.fromNbt(this, nbt.getCompound(NBT_REAL_TIMES));
        this.availableDepartureBufferTime = nbt.getLong(NBT_CURRENT_TICKS_CORRECTION);
        this.cycle = nbt.getInt(NBT_CYCLE);
        this.transitTime.forceValue(nbt.getInt(NBT_TRANSIT_TIME));
        this.averageStayDuration = nbt.getInt(NBT_AVERAGE_STAY_DURATION);
    }

    /**
     * DEBUG ONLY!
     */
    public Component formattedText() {
        return TextUtils.text("[ " + entryIndex + " ]: ").withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(getStationName()).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("*" + getCurrentCycle()).withStyle(ChatFormatting.YELLOW))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("sA: " + (getScheduledArrivalTime())).withStyle(ChatFormatting.BLUE))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("rA: " + getRealTimeArrivalTime()).withStyle(ChatFormatting.GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("d: " + (getArrivalTimeDeviation() + " / " + getDepartureTimeDeviation())).withStyle(ChatFormatting.GOLD))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("B: " + (getBufferTime())).withStyle(ChatFormatting.DARK_GREEN))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("W: " + getStayDuration() + " / " + getMinStayDuration()).withStyle(ChatFormatting.AQUA))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("S: " + getSection()).withStyle(ChatFormatting.RED))
            .append(TextUtils.text(", ").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text("T: " + title).withStyle(ChatFormatting.LIGHT_PURPLE))
        ;
    }

    public void shiftTime(long l) {
        DLUtils.doIfNotNull(this.scheduled(), x -> x.shift(l, true));
        DLUtils.doIfNotNull(this.realTime(), x -> x.shift(l, true));
    }

    @Override
    public int compareTo(TrainPrediction o) {
        return Long.compare(getScheduledArrivalTime(), o.getScheduledArrivalTime());
    }
}
