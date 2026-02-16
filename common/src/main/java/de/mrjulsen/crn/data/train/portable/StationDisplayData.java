package de.mrjulsen.crn.data.train.portable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StationDisplayData {

    public enum State {
        OUT_OF_SERVICE(0),
        APPROACHING(1),
        WAITING(2);

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static State getById(int id) {
            return Arrays.stream(values()).filter(x -> x.id() == id).findFirst().orElse(OUT_OF_SERVICE);
        }

        public boolean isOutOfService() {
            return this == OUT_OF_SERVICE;
        }

        public boolean isApproaching(boolean includeSoftTerminus) {
            return this == APPROACHING;
        }

        public boolean isWaiting() {
            return this == WAITING;
        }
    }

    private final BasicTrainDisplayData trainData;
    private final TrainStopDisplayData stationData;
    private final String firstStopName;
    private final boolean isFirstStop;
    private final boolean isLastStop;
    private final boolean isNextSectionExcluded;
    private final boolean isPrevSectionExcluded;
    private final List<String> stopovers;
    private final State state;

    private static final String NBT_TRAIN = "Train";
    private static final String NBT_STATION = "Station";
    private static final String NBT_STOPOVERS = "Stopovers";
    private static final String NBT_FIRST_STOP = "FirstStop";
    private static final String NBT_IS_FIRST = "IsFirst";
    private static final String NBT_IS_LAST = "IsLast";
    private static final String NBT_SHOW_ARRIVAL = "ShowArrival";
    private static final String NBT_IS_NEXT_EXCLUDED = "IsNextSectionExcluded";
    private static final String NBT_IS_PREV_EXCLUDED = "IsPrevSectionExcluded";
    private static final String NBT_STATE = "State";

    

    public StationDisplayData(
        BasicTrainDisplayData trainData,
        TrainStopDisplayData stationData,
        String firstStopName,
        boolean isFirstStop,
        boolean isLastStop,
        boolean isNextSectionExcluded,
        boolean isPrevSectionExcluded,
        List<String> stopovers,
        State state
    ) {
        this.trainData = trainData;
        this.stationData = stationData;
        this.stopovers = stopovers;
        this.firstStopName = firstStopName;
        this.isFirstStop = isFirstStop;
        this.isLastStop = isLastStop;
        this.isNextSectionExcluded = isNextSectionExcluded;
        this.isPrevSectionExcluded = isPrevSectionExcluded;
        this.state =  state;
    }

    public static StationDisplayData empty() {
        return new StationDisplayData(BasicTrainDisplayData.empty(), TrainStopDisplayData.empty(), "", false, false, false, false, List.of(), State.OUT_OF_SERVICE);
    }

    /** Server-side only! */
    public static StationDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }

        return TrainListener.getTrainData(stop.getTrainId()).map(data -> {
            ScheduleSection section = data.getSectionByIndex(stop.getSectionIndex());
            ScheduleSection previousSection = section.previousSection();

            boolean isFirstStopOfSection = section.getFirstStop().isPresent() && section.getFirstStop().get().getEntryIndex() == stop.getScheduleIndex();
            
            ScheduleSection targetedSection = section;
            boolean isLastStopOfSection = false;
            if (section.getFinalStop().isPresent()) {
                if (isFirstStopOfSection && previousSection.shouldIncludeNextStationOfNextSection() && previousSection.getFinalStop().isPresent()) {
                    targetedSection = previousSection;
                }
                isLastStopOfSection = targetedSection.getFinalStop().get().getEntryIndex() == stop.getScheduleIndex();
            }

            ScheduleSection sectionForOrigin = section;
            if (isFirstStopOfSection) {
                sectionForOrigin = previousSection;
            }

            String firstStop = sectionForOrigin.getFirstStop().isPresent() ? sectionForOrigin.getFirstStop().get().getStationTag().getTagName().get() : "?";

            State state;
            if (!(data.waitingAtStationIndex == stop.getScheduleIndex())) {
                state = State.APPROACHING;
            } else {
                state = State.WAITING;
            }

            return new StationDisplayData(
                BasicTrainDisplayData.of(stop),
                TrainStopDisplayData.of(stop),
                firstStop,
                isFirstStopOfSection,
                isLastStopOfSection,
                isLastStopOfSection && (!targetedSection.nextSection().isUsable() || !targetedSection.shouldIncludeNextStationOfNextSection()),
                isFirstStopOfSection && (!previousSection.isUsable()),
                section.getStopoversFrom(stop.getScheduleIndex()),
                state
            );
        }).orElse(empty());
    }

    public BasicTrainDisplayData getTrainData() {
        return trainData;
    }

    public TrainStopDisplayData getStationData() {
        return stationData;
    }

    public List<String> getStopovers() {
        return stopovers;
    }

    public String getFirstStopName() {
        return firstStopName;
    }

    public boolean isFirstStop() {
        return isFirstStop;
    }

    public boolean isLastStop() {
        return isLastStop;
    }

    public State getState() {
        return state;
    }

    /**
     * @return Returns only {@code true} if this is the last stop of the current section and the next section is not navigable.
     */
    public boolean isNextSectionExcluded() {
        return isNextSectionExcluded;
    }

    /**
     * @return Returns only {@code true} if this is the first stop of the current section and the previous section was not navigable.
     */
    public boolean isPrevSectionExcluded() {
        return isPrevSectionExcluded;
    }

    public boolean isDelayed() {
        return isLastStop() ? getStationData().isArrivalDelayed() : getStationData().isDepartureDelayed();
    }

    public long getScheduledTime() {
        return isLastStop() ? getStationData().getScheduledArrivalTime() : getStationData().getScheduledDepartureTime();
    }

    public long getRealTime() {
        return isLastStop() ? getStationData().getRealTimeArrivalTime() : getStationData().getRealTimeDepartureTime();
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag stopoversList = new ListTag();
        List<String> stations = getStopovers();
        for (String name : stations) {
            stopoversList.add(StringTag.valueOf(name));
        }

        nbt.put(NBT_TRAIN, trainData.toNbt());
        nbt.put(NBT_STATION, stationData.toNbt());
        nbt.putString(NBT_FIRST_STOP, firstStopName);
        nbt.putBoolean(NBT_IS_FIRST, isFirstStop);
        nbt.putBoolean(NBT_IS_LAST, isLastStop);
        nbt.putBoolean(NBT_IS_NEXT_EXCLUDED, isNextSectionExcluded);
        nbt.putBoolean(NBT_IS_PREV_EXCLUDED, isPrevSectionExcluded);
        nbt.put(NBT_STOPOVERS, stopoversList);
        nbt.putInt(NBT_STATE, state.id());
        return nbt;
    }

    public static StationDisplayData fromNbt(CompoundTag nbt) {
        return new StationDisplayData(
            BasicTrainDisplayData.fromNbt(nbt.getCompound(NBT_TRAIN)),
            TrainStopDisplayData.fromNbt(nbt.getCompound(NBT_STATION)),
            nbt.getString(NBT_FIRST_STOP),
            nbt.getBoolean(NBT_IS_FIRST),
            nbt.getBoolean(NBT_IS_LAST),
            nbt.getBoolean(NBT_IS_NEXT_EXCLUDED),
            nbt.getBoolean(NBT_IS_PREV_EXCLUDED),
            nbt.getList(NBT_STOPOVERS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList(),
            State.getById(nbt.getInt(NBT_STATE))
        );
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof StationDisplayData o && o.getTrainData().equals(getTrainData()) && o.getStationData().equals(getStationData());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getTrainData(), getStationData());
    }
}
