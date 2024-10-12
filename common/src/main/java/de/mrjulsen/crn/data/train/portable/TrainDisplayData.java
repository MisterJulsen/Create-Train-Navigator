package de.mrjulsen.crn.data.train.portable;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class TrainDisplayData {
    private final BasicTrainDisplayData trainData;
    private final List<TrainStopDisplayData> stops;
    private final int currentScheduleIndex;
    private final double speed;
    private final boolean oppositeDirection;
    private final TrainExitSide exitSide;
    private final boolean isWaitingAtStation;
    private final boolean empty;

    private final Cache<List<TrainStopDisplayData>> stopsFromHere;
    private final Cache<List<TrainStopDisplayData>> stopovers;

    private static final String NBT_TRAIN = "Train";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_INDEX = "CurrentIndex";
    private static final String NBT_SPEED = "Speed";
    private static final String NBT_OPPOSITE_DIRECTION = "Opposite";
    private static final String NBT_EXIT_SIDE = "ExitSide";
    private static final String NBT_AT_STATION = "AtStation";
    private static final String NBT_IS_EMPTY = "Empty";
    

    private TrainDisplayData() {
        this.trainData = BasicTrainDisplayData.empty();
        this.stops = List.of();
        this.currentScheduleIndex = -1;
        this.speed = 0;
        this.oppositeDirection = false;
        this.exitSide = TrainExitSide.UNKNOWN;
        this.stopsFromHere = new Cache<>(() -> List.of());
        this.stopovers = new Cache<>(() -> List.of());
        this.isWaitingAtStation = false;
        this.empty = true;
    }

    public TrainDisplayData(
        BasicTrainDisplayData trainData,
        List<TrainStopDisplayData> stops,
        int currentScheduleIndex,
        TrainExitSide exitSide,
        double speed,
        boolean oppositeDirection,
        boolean isWaitingAtStation,
        boolean empty
    ) {
        this.trainData = trainData;
        this.stops = stops;
        this.currentScheduleIndex = currentScheduleIndex;
        this.speed = speed;
        this.oppositeDirection = oppositeDirection;
        this.exitSide = exitSide;
        this.isWaitingAtStation = isWaitingAtStation;
        this.stopsFromHere = new Cache<>(() -> {
            boolean startFound = false;
            List<TrainStopDisplayData> list = new ArrayList<>();
            for (TrainStopDisplayData stop : getAllStops()) {
                if (stop.getStationEntryIndex() == getCurrentScheduleIndex()) startFound = true;
                if (!startFound) continue;
                list.add(stop);
            }
            return list;
        });
        this.stopovers = new Cache<>(() -> getStopsFromCurrentStation().size() > 2 ? getStopsFromCurrentStation().stream().limit(getStopsFromCurrentStation().size() - 1).skip(1).toList() : List.of());
        this.empty = empty;
    }

    public static TrainDisplayData empty() {
        return new TrainDisplayData();
    }

    /** Server-side only! */
    public static TrainDisplayData of(Train train) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        if (!TrainListener.data.containsKey(train.id) || train.runtime.getSchedule() == null) {
            return empty();
        }

        MutableSingle<TrainExitSide> sideHolder = new MutableSingle<>(null); 
        ModCommonEvents.getCurrentServer().ifPresent(x -> {
            x.execute(() -> sideHolder.setFirst(TrainUtils.getExitSide(train.navigation.destination)));
            while (sideHolder.getFirst() == null) {
                try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) {}
            }
        });
        TrainExitSide side = sideHolder.getFirst() == null ? TrainExitSide.UNKNOWN : sideHolder.getFirst();

        TrainData data = TrainListener.data.get(train.id);
        TrainTravelSection section = data.getCurrentSection();
        return new TrainDisplayData(
            BasicTrainDisplayData.of(train.id),
            section.isUsable() ? data.getCurrentSection().getPredictions(-1, false).stream().map(x -> TrainStopDisplayData.of(new TrainStop(x))).toList() : List.of(),
            data.getCurrentScheduleIndex(),
            side,
            train.speed,
            train.currentlyBackwards,
            data.isWaitingAtStation(),
            !section.isUsable()
        );
    }

    public BasicTrainDisplayData getTrainData() {
        return trainData;
    }

    public List<TrainStopDisplayData> getAllStops() {
        return stops;
    }

    public List<TrainStopDisplayData> getStopsFromCurrentStation() {
        return stopsFromHere.get();
    }

    public List<TrainStopDisplayData> getStopovers() {
        return stopovers.get();
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isOppositeDirection() {
        return oppositeDirection;
    }
    
    public TrainExitSide getNextStopExitSide() {
        return exitSide;
    }

    public boolean isWaitingAtStation() {
        return isWaitingAtStation;
    }

    public boolean isEmpty() {
        return empty;
    }

    public int getCurrentScheduleIndex() {
        return currentScheduleIndex;
    }

    public Optional<TrainStopDisplayData> getNextStop() {
        return !getStopsFromCurrentStation().isEmpty() ? Optional.of(getStopsFromCurrentStation().get(0)) : Optional.empty();
    }

    public Optional<TrainStopDisplayData> getLastStop() {
        return !getStopsFromCurrentStation().isEmpty() ? Optional.of(getStopsFromCurrentStation().get(getStopsFromCurrentStation().size() - 1)) : Optional.empty();
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag stopsList = new ListTag();
        stopsList.addAll(getAllStops().stream().map(x -> x.toNbt()).toList());

        nbt.put(NBT_TRAIN, trainData.toNbt());
        nbt.put(NBT_STOPS, stopsList);
        nbt.putInt(NBT_INDEX, currentScheduleIndex);
        nbt.putDouble(NBT_SPEED, speed);
        nbt.putBoolean(NBT_OPPOSITE_DIRECTION, oppositeDirection);
        nbt.putByte(NBT_EXIT_SIDE, exitSide.getAsByte());
        nbt.putBoolean(NBT_AT_STATION, isWaitingAtStation);
        nbt.putBoolean(NBT_IS_EMPTY, isEmpty());
        return nbt;
    }

    public static TrainDisplayData fromNbt(CompoundTag nbt) {
        if (nbt.getBoolean(NBT_IS_EMPTY)) {
            return new TrainDisplayData();
        }
        return new TrainDisplayData(
            BasicTrainDisplayData.fromNbt(nbt.getCompound(NBT_TRAIN)),
            nbt.getList(NBT_STOPS, Tag.TAG_COMPOUND).stream().map(x -> TrainStopDisplayData.fromNbt((CompoundTag)x)).toList(),
            nbt.getInt(NBT_INDEX),
            TrainExitSide.getFromByte(nbt.getByte(NBT_EXIT_SIDE)),
            nbt.getDouble(NBT_SPEED),
            nbt.getBoolean(NBT_OPPOSITE_DIRECTION),
            nbt.getBoolean(NBT_AT_STATION),
            nbt.getBoolean(NBT_IS_EMPTY)
        );
    }
}
