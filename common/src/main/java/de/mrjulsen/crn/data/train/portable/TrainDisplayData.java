package de.mrjulsen.crn.data.train.portable;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

public class TrainDisplayData {

    public enum State {
        RUNNING(0),
        OUT_OF_SERVICE(1),
        AT_TERMINUS(2),
        BEFORE_TERMINUS(3),
        TERMINUS_ANNOUNCED(4),
        SOFT_TERMINUS_ANNOUNCED(5),
        BEFORE_START(6);

        private final int id;

        private State(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static State getById(int id) {
            return Arrays.stream(values()).filter(x -> x.id() == id).findFirst().orElse(OUT_OF_SERVICE);
        }

        public boolean isOutOfService() {
            return this == OUT_OF_SERVICE || this == BEFORE_START;
        }

        public boolean isTerminating(boolean includeSoftTerminus) {
            return this == AT_TERMINUS || this == BEFORE_TERMINUS || this == TERMINUS_ANNOUNCED || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus);
        }

        public boolean isAboutToStart() {
            return this == BEFORE_START;
        }

        public boolean shouldNotBoard(boolean includeSoftTerminus) {
            return this == AT_TERMINUS || this == TERMINUS_ANNOUNCED || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus) || isAboutToStart();
        }

        public boolean isIrregular(boolean includeSoftTerminus) {
            return shouldNotBoard(includeSoftTerminus) || isAboutToStart() || isOutOfService();
        }
    }
    

    private final BasicTrainDisplayData trainData;
    private final List<TrainStopDisplayData> stops;
    private final int currentScheduleIndex;
    private final double speed;
    private final boolean oppositeDirection;
    private final TrainExitSide exitSide;
    private final boolean isWaitingAtStation;
    private final State state;

    private final Cache<Pair<Integer, List<TrainStopDisplayData>>> stopsFromHere;
    private final Cache<List<TrainStopDisplayData>> stopovers;

    private static final String NBT_TRAIN = "Train";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_INDEX = "CurrentIndex";
    private static final String NBT_SPEED = "Speed";
    private static final String NBT_OPPOSITE_DIRECTION = "Opposite";
    private static final String NBT_EXIT_SIDE = "ExitSide";
    private static final String NBT_AT_STATION = "AtStation";
    private static final String NBT_OUT_OF_SERVICE = "OutOfService";
    private static final String NBT_DO_NOT_BOARD = "DoNotBoard";
    private static final String NBT_STATE = "State";

    private TrainDisplayData() {
        this.trainData = BasicTrainDisplayData.empty();
        this.stops = List.of();
        this.currentScheduleIndex = -1;
        this.speed = 0;
        this.oppositeDirection = false;
        this.exitSide = TrainExitSide.UNKNOWN;
        this.stopsFromHere = new Cache<>(() -> Pair.of(0, List.of()));
        this.stopovers = new Cache<>(() -> List.of());
        this.isWaitingAtStation = false;
        this.state = State.OUT_OF_SERVICE;
    }

    public TrainDisplayData(
        BasicTrainDisplayData trainData,
        List<TrainStopDisplayData> stops,
        int currentScheduleIndex,
        TrainExitSide exitSide,
        double speed,
        boolean oppositeDirection,
        boolean isWaitingAtStation,
        State state
    ) {
        this.trainData = trainData;
        this.stops = stops;
        this.currentScheduleIndex = currentScheduleIndex;
        this.speed = speed;
        this.oppositeDirection = oppositeDirection;
        this.exitSide = exitSide;
        this.stopsFromHere = new Cache<>(() -> {
            boolean startFound = false;
            List<TrainStopDisplayData> list = new ArrayList<>();
            int targetIndex = getCurrentScheduleIndex();
            int idx = 0;
            int startIndex = -1;
            int lastIdx = -1;
            
            for (int i = 0; i < getAllStops().size(); i++) {
                TrainStopDisplayData stop = getAllStops().get(i);
                int stopIdx = stop.getStationEntryIndex();
                if (startIndex < 0) {
                    startIndex = stopIdx;
                }
                if (lastIdx > stopIdx) {
                    startIndex = 0;
                }
                if (!startFound && targetIndex >= startIndex && stopIdx >= targetIndex) {
                    startFound = true;
                    idx = i;
                }
                lastIdx = stopIdx;
                
                if (!startFound) continue;
                list.add(stop);
            }
            return Pair.of(idx, list);
        });
        this.isWaitingAtStation = isWaitingAtStation && (this.stopsFromHere.get().getSecond().isEmpty() || this.stopsFromHere.get().getSecond().get(0).getStationEntryIndex() == getCurrentScheduleIndex());
        this.stopovers = new Cache<>(() -> getStopsFromCurrentStation().size() > (isWaitingAtStation ? 2 : 1) ? getStopsFromCurrentStation().stream().limit(getStopsFromCurrentStation().size() - 1).skip(isWaitingAtStation ? 1 : 0).toList() : List.of());
        this.state = state;
    }

    public static TrainDisplayData empty() {
        return new TrainDisplayData();
    }

    /** Server-side only! */
    public static TrainDisplayData of(Train train) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        if (train.runtime.getSchedule() == null) {
            return empty();
        }

        return TrainListener.getTrainData(train.id).map(data -> {
            MutableHolder<TrainExitSide> sideHolder = new MutableHolder<>(null); 

            MinecraftServer server = ModCommonEvents.getCurrentServer().orElse(null);
            if (server != null) {
                if (Thread.currentThread() == server.getRunningThread()) {
                    sideHolder.set(TrainUtils.getExitSide(train.navigation.destination));
                } else {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    server.execute(() -> {
                        sideHolder.set(TrainUtils.getExitSide(train.navigation.destination));
                        future.complete(null);
                    });
                    future.join();
                }
            }
            TrainExitSide side = sideHolder.get() == null ? TrainExitSide.UNKNOWN : sideHolder.get();

            final ScheduleSection section = data.getCurrentSection();
            final ScheduleSection prevSection = section.previousSection();
            ScheduleSection selectedSection = section;

            boolean isAtStation = data.waitingAtStationIndex == data.getCurrentScheduleIndex();
            boolean isFirstStationInSection = section.getFirstStop().map(x -> x.getEntryIndex() == data.getCurrentScheduleIndex()).orElse(false);
            boolean isLastStationInSection;
            if (isFirstStationInSection) {
                isLastStationInSection = prevSection.isUsable() && prevSection.getFinalStop().map(x -> x.getEntryIndex() == data.getCurrentScheduleIndex()).orElse(false);
            } else {
                isLastStationInSection = section.isUsable() && section.getFinalStop().map(x -> x.getEntryIndex() == data.getCurrentScheduleIndex()).orElse(false);
            }

            if (isFirstStationInSection && !isAtStation && prevSection.shouldIncludeNextStationOfNextSection()) {
                selectedSection = prevSection;
            }

            List<TrainStopDisplayData> stopsOfSection = new ArrayList<>();
            if (selectedSection.isUsable()) {
                List<TrainPrediction> predictions = selectedSection.getPredictions(-1, false);
                for (int i = 0; i < predictions.size(); i++) {
                    TrainPrediction prediction = predictions.get(i);
                    TrainStop stop = new TrainStop(prediction);
                    if (i == predictions.size() - 1 && predictions.get(0) == prediction) {
                        stop.simulateCycles(1);
                    }
                    stopsOfSection.add(TrainStopDisplayData.of(stop));
                }
            }
            boolean preStart = isFirstStationInSection && (!prevSection.shouldIncludeNextStationOfNextSection() || !prevSection.isUsable()) && !isAtStation;
            boolean nextStopTerminus = false;
            if (isLastStationInSection) {
                if (isFirstStationInSection) {
                    nextStopTerminus = !prevSection.shouldIncludeNextStationOfNextSection() || !section.isUsable() || !isAtStation;
                } else {
                    nextStopTerminus = (!section.shouldIncludeNextStationOfNextSection() || !section.nextSection().isUsable());
                }
            }
            boolean atTerminus = nextStopTerminus && isAtStation;
            boolean teminusAnnounced = nextStopTerminus && data.getNextStopPrediction().map(x -> x.realTime().arrivalIn() < ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()).orElse(false);

            State state = State.OUT_OF_SERVICE;
            if (preStart) state = State.BEFORE_START;
            else if (atTerminus) state = State.AT_TERMINUS;
            else if (teminusAnnounced) {
                if (isLastStationInSection && isFirstStationInSection && prevSection.shouldIncludeNextStationOfNextSection() && section.isUsable()) {
                    state = State.SOFT_TERMINUS_ANNOUNCED;
                } else {
                    state = State.TERMINUS_ANNOUNCED;
                }
            }
            else if (nextStopTerminus) state = State.BEFORE_TERMINUS;
            else if (selectedSection.isUsable())   state = State.RUNNING;

            return new TrainDisplayData(
                BasicTrainDisplayData.of(train.id),
                stopsOfSection,
                data.getCurrentScheduleIndex(),
                side,
                train.speed,
                train.currentlyBackwards,
                isAtStation,
                state
            );
        }).orElse(empty());        
    }

    public BasicTrainDisplayData getTrainData() {
        return trainData;
    }

    public List<TrainStopDisplayData> getAllStops() {
        return stops;
    }

    public List<TrainStopDisplayData> getStopsFromCurrentStation() {
        return stopsFromHere.get().getSecond();
    }

    public int getCurrentStopIndex() {
        return stopsFromHere.get().getFirst();
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
    
    public State getState() {
        return state;
    }

    public int getCurrentScheduleIndex() {
        return currentScheduleIndex;

    }

    public Optional<TrainStopDisplayData> getCurrentStop() {
        int idx = getCurrentStopIndex();
        if (!isWaitingAtStation()) {
            idx -= 1;
            if (idx < 0) {
                idx = getAllStops().size() - 1;
            }
        }
        if (idx < 0 || idx >= getAllStops().size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getAllStops().get(idx));
    }

    public Optional<TrainStopDisplayData> getNextStop() {
        return !getStopsFromCurrentStation().isEmpty() ? Optional.of(getStopsFromCurrentStation().get(0)) : Optional.empty();
    }

    public Optional<TrainStopDisplayData> getFinalStop() {
        return !getStopsFromCurrentStation().isEmpty() ? Optional.of(getStopsFromCurrentStation().get(getStopsFromCurrentStation().size() - 1)) : Optional.empty();
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag stopsList = new ListTag();
        List<TrainStopDisplayData> allStops = getAllStops();
        for (TrainStopDisplayData stop : allStops) {
            stopsList.add(stop.toNbt());
        }

        nbt.put(NBT_TRAIN, trainData.toNbt());
        nbt.put(NBT_STOPS, stopsList);
        nbt.putInt(NBT_INDEX, currentScheduleIndex);
        nbt.putDouble(NBT_SPEED, speed);
        nbt.putBoolean(NBT_OPPOSITE_DIRECTION, oppositeDirection);
        nbt.putByte(NBT_EXIT_SIDE, exitSide.getAsByte());
        nbt.putBoolean(NBT_AT_STATION, isWaitingAtStation);
        nbt.putInt(NBT_STATE, state.id());
        return nbt;
    }

    public static TrainDisplayData fromNbt(CompoundTag nbt) {
        
        if (nbt.getBoolean(NBT_OUT_OF_SERVICE) && !nbt.getBoolean(NBT_DO_NOT_BOARD)) {
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
            State.getById(nbt.getInt(NBT_STATE))
        );
    }
}
