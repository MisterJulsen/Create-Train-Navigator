package de.mrjulsen.crn.data.train.portable;

import java.util.List;
import java.util.Objects;

import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StationDisplayData {
    private final BasicTrainDisplayData trainData;
    private final TrainStopDisplayData stationData;
    private final String firstStopName;
    private final boolean isLastStop;
    private final List<String> stopovers;

    private static final String NBT_TRAIN = "Train";
    private static final String NBT_STATION = "Station";
    private static final String NBT_STOPOVERS = "Stopovers";
    private static final String NBT_FIRST_STOP = "FirstStop";
    private static final String NBT_IS_LAST = "IsLast";

    

    public StationDisplayData(
        BasicTrainDisplayData trainData,
        TrainStopDisplayData stationData,
        String firstStopName,
        boolean isLastStop,
        List<String> stopovers
    ) {
        this.trainData = trainData;
        this.stationData = stationData;
        this.stopovers = stopovers;
        this.firstStopName = firstStopName;
        this.isLastStop = isLastStop;
    }

    public static StationDisplayData empty() {
        return new StationDisplayData(BasicTrainDisplayData.empty(), TrainStopDisplayData.empty(), "", false, List.of());
    }

    /** Server-side only! */
    public static StationDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        if (!TrainListener.data.containsKey(stop.getTrainId())) {
            return empty();
        }
        TrainData data = TrainListener.data.get(stop.getTrainId());
        TrainTravelSection section = data.getSectionByIndex(stop.getSectionIndex());
        String firstStop = section.getFirstStop().isPresent() ? section.getFirstStop().get().getStationTag().getTagName().get() : "";
        boolean isLastStopOfSection = section.getFinalStop().isPresent() && section.getFinalStop().get().getEntryIndex() == stop.getScheduleIndex();
        if (isLastStopOfSection) {
            TrainTravelSection nextSection = section.nextSection();
            if (nextSection.shouldIncludeLastStationOfLastSection() && nextSection.getFirstStop().isPresent() && nextSection.getFirstStop().get().getEntryIndex() == stop.getScheduleIndex()) {
                section = nextSection;
                isLastStopOfSection = !(data.isWaitingAtStation() && data.getCurrentScheduleIndex() == stop.getScheduleIndex());
            }
        }
        return new StationDisplayData(
            BasicTrainDisplayData.of(stop),
            TrainStopDisplayData.of(stop),
            firstStop,
            isLastStopOfSection,
            isLastStopOfSection && section.nextSection().isUsable() ? section.nextSection().getStopoversFrom(stop.getScheduleIndex()) : section.getStopoversFrom(stop.getScheduleIndex())
        );
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

    public boolean isLastStop() {
        return isLastStop;
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
        stopoversList.addAll(getStopovers().stream().map(x -> StringTag.valueOf(x)).toList());

        nbt.put(NBT_TRAIN, trainData.toNbt());
        nbt.put(NBT_STATION, stationData.toNbt());
        nbt.putString(NBT_FIRST_STOP, firstStopName);
        nbt.putBoolean(NBT_IS_LAST, isLastStop);
        nbt.put(NBT_STOPOVERS, stopoversList);
        return nbt;
    }

    public static StationDisplayData fromNbt(CompoundTag nbt) {
        return new StationDisplayData(
            BasicTrainDisplayData.fromNbt(nbt.getCompound(NBT_TRAIN)),
            TrainStopDisplayData.fromNbt(nbt.getCompound(NBT_STATION)),
            nbt.getString(NBT_FIRST_STOP),
            nbt.getBoolean(NBT_IS_LAST),
            nbt.getList(NBT_STOPOVERS, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList()
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
