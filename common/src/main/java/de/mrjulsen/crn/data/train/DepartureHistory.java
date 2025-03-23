package de.mrjulsen.crn.data.train;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.nbt.CompoundTag;

public final class DepartureHistory {   

    public static class Data {

        private static final String NBT_LAST_DEPARTURE = "LastDeparture";
        private static final String NBT_LINES = "Lines";
        private static final String NBT_GROUPS = "Groups";
        private static final String NBT_NAMES = "Names";

        private long lastDepartureTime = Long.MIN_VALUE;
        private Map<TrainLine, Long> lastDepartureByLine = new ConcurrentHashMap<>();
        private Map<TrainGroup, Long> lastDepartureByGroup = new ConcurrentHashMap<>();
        private Map<String, Long> lastDepartureByTrainName = new ConcurrentHashMap<>();

        public void setDeparture(Train train) {
            this.lastDepartureTime = DragonLib.getCurrentServer().get().overworld().getGameTime();
            this.lastDepartureByTrainName.put(train.name.getString(), this.lastDepartureTime);
            TrainListener.getTrainData(train.id).ifPresent(data -> {
                TrainTravelSection section = data.getCurrentSection();
                section.getTrainLine().ifPresent(x -> this.lastDepartureByLine.put(x, this.lastDepartureTime));
                section.getTrainGroup().ifPresent(x -> this.lastDepartureByGroup.put(x, this.lastDepartureTime));
            });
        }

        public long getLastDepartureTime(ETrainFilter filter, String trainName, @Nullable TrainTravelSection section) {
            return switch (filter) {
                case SAME_GROUP -> section != null ? section.getTrainGroup().map(x -> lastDepartureByGroup.getOrDefault(x, Long.MIN_VALUE)).orElse(Long.MIN_VALUE) : Long.MIN_VALUE;
                case SAME_LINE -> section != null ? section.getTrainLine().map(x -> lastDepartureByLine.getOrDefault(x, Long.MIN_VALUE)).orElse(Long.MIN_VALUE) : Long.MIN_VALUE;
                case SAME_NAME -> trainName != null ? lastDepartureByTrainName.getOrDefault(trainName, Long.MIN_VALUE) : Long.MIN_VALUE;
                default -> lastDepartureTime;
            };
        }

        public long getLastDepartureTime() {
            return lastDepartureTime;
        }

        public Map<TrainLine, Long> getLastDeparturesByLine() {
            return lastDepartureByLine;
        }

        public Map<TrainGroup, Long> getLastDeparturesByGroup() {
            return lastDepartureByGroup;
        }

        public Map<String, Long> getLastDeparturesByTrainName() {
            return lastDepartureByTrainName;
        }

        public Optional<Long> getDepartureByGroup(TrainGroup group) {
            return Optional.ofNullable(lastDepartureByGroup.containsKey(group) ? lastDepartureByGroup.get(group) : null);
        }

        public Optional<Long> getDepartureByLine(TrainLine line) {
            return Optional.ofNullable(lastDepartureByLine.containsKey(line) ? lastDepartureByLine.get(line) : null);
        }

        public Optional<Long> getDepartureByName(String name) {
            return Optional.ofNullable(lastDepartureByTrainName.containsKey(name) ? lastDepartureByTrainName.get(name) : null);
        }

        public long debug_cachedDataCount() {
            return 1 + lastDepartureByLine.size() + lastDepartureByGroup.size();
        }
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong(NBT_LAST_DEPARTURE, lastDepartureTime);
            
            CompoundTag linesList = new CompoundTag();
            for (Map.Entry<TrainLine, Long> e : lastDepartureByLine.entrySet()) {
                linesList.putLong(e.getKey().getLineName(), e.getValue());
            }
            nbt.put(NBT_LINES, linesList);
            
            CompoundTag groupsList = new CompoundTag();
            for (Map.Entry<TrainGroup, Long> e : lastDepartureByGroup.entrySet()) {
                groupsList.putLong(e.getKey().getGroupName(), e.getValue());
            }
            nbt.put(NBT_GROUPS, groupsList);
            
            CompoundTag namesList = new CompoundTag();
            for (Map.Entry<String, Long> e : lastDepartureByTrainName.entrySet()) {
                namesList.putLong(e.getKey(), e.getValue());
            }
            nbt.put(NBT_NAMES, namesList);
            return nbt;
        }

        public static Data fromNbt(CompoundTag nbt) {
            Data data = new Data();
            data.lastDepartureTime = nbt.getLong(NBT_LAST_DEPARTURE);
            CompoundTag linesList = nbt.getCompound(NBT_LINES);
            for (String key : linesList.getAllKeys()) {
                GlobalSettings.getInstance().getTrainLine(key).ifPresent(x -> data.lastDepartureByLine.put(x, linesList.getLong(key)));
            }
            CompoundTag groupsList = nbt.getCompound(NBT_GROUPS);
            for (String key : groupsList.getAllKeys()) {
                GlobalSettings.getInstance().getTrainGroup(key).ifPresent(x -> data.lastDepartureByGroup.put(x, groupsList.getLong(key)));
            }
            CompoundTag namesList = nbt.getCompound(NBT_NAMES);
            for (String key : namesList.getAllKeys()) {
                data.lastDepartureByTrainName.put(key, namesList.getLong(key));
            }
            return data;
        }
    }

    public static enum ETrainFilter implements ITranslatableEnum {
        ANY((byte)0, "any"),
        SAME_LINE((byte)1, "same_line"),
        SAME_GROUP((byte)2, "same_group"),
        SAME_NAME((byte)3, "same_name");

        private final byte index;
        private final String name;

        private ETrainFilter(byte index, String name) {
            this.index = index;
            this.name = name;
        }

        public byte getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public static ETrainFilter getByIndex(byte i) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == i).findFirst().orElse(ANY);
        }

		@Override
		public String getEnumName() {
			return "train_filter";
		}

		@Override
		public String getEnumValueName() {
			return name;
		}
    }

    public static class Stats {

        private static final String NBT_LAST_DEPARTURE = "LastDeparture";
        private static final String NBT_LINE = "Line";
        private static final String NBT_GROUP = "Group";
        private static final String NBT_NAME = "Name";

        private final long lastDeparture;
        private final Map<String, Long> departuresByGroup;
        private final Map<String, Long> departuresByLine;
        private final Map<String, Long> departuresByName;

        private final Optional<Pair<String, Long>> latestGroupDeparture;
        private final Optional<Pair<String, Long>> latestLineDeparture;
        private final Optional<Pair<String, Long>> latestNameDeparture;

        public Stats(long lastDeparture, Map<String, Long> departuresByGroup, Map<String, Long> departuresByLine, Map<String, Long> departuresByName) {
            this.lastDeparture = lastDeparture;
            this.departuresByGroup = departuresByGroup;
            this.departuresByLine = departuresByLine;
            this.departuresByName = departuresByName;

            this.latestGroupDeparture = departuresByGroup.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
            this.latestLineDeparture = departuresByLine.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
            this.latestNameDeparture = departuresByName.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
        }

        public static Stats of(Data data) {
            Map<String, Long> groups = data.getLastDeparturesByGroup().entrySet().stream().collect(Collectors.toMap(p -> p.getKey().getGroupName(), p -> p.getValue()));
            Map<String, Long> lines = data.getLastDeparturesByLine().entrySet().stream().collect(Collectors.toMap(p -> p.getKey().getLineName(), p -> p.getValue()));
            Map<String, Long> names = data.getLastDeparturesByTrainName().entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            return new Stats(data.getLastDepartureTime(), groups, lines, names);
        }

        public static Stats ofStation(String stationName) {
            return getDeparturesAtStation(stationName).values().stream().map(Stats::of).findFirst().orElse(Stats.empty());
        }        

        public static Stats empty() {
            return new Stats(-1, Map.of(), Map.of(), Map.of());
        }

        public boolean isEmpty() {
            return getLastDeparture() < 0 && getDeparturesByGroup().isEmpty() && getDeparturesByLine().isEmpty() && getDeparturesByName().isEmpty();
        }

        public long getLastDeparture() {
            return lastDeparture;
        }

        public Map<String, Long> getDeparturesByGroup() {
            return departuresByGroup;
        }

        public Map<String, Long> getDeparturesByLine() {
            return departuresByLine;
        }

        public Map<String, Long> getDeparturesByName() {
            return departuresByName;
        }

        public Optional<Pair<String, Long>> getLatestGroupDeparture() {
            return latestGroupDeparture;
        }

        public Optional<Pair<String, Long>> getLatestLineDeparture() {
            return latestLineDeparture;
        }

        public Optional<Pair<String, Long>> getLatestNameDeparture() {
            return latestNameDeparture;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            
            CompoundTag groupsTag = new CompoundTag();
            for (Map.Entry<String, Long> e : departuresByGroup.entrySet()) {
                groupsTag.putLong(e.getKey(), e.getValue());
            }
            CompoundTag linesTag = new CompoundTag();
            for (Map.Entry<String, Long> e : departuresByLine.entrySet()) {
                linesTag.putLong(e.getKey(), e.getValue());
            }
            CompoundTag namesTag = new CompoundTag();
            for (Map.Entry<String, Long> e : departuresByName.entrySet()) {
                namesTag.putLong(e.getKey(), e.getValue());
            }
            nbt.putLong(NBT_LAST_DEPARTURE, lastDeparture);
            nbt.put(NBT_GROUP, groupsTag);
            nbt.put(NBT_LINE, linesTag);
            nbt.put(NBT_NAME, namesTag);
            return nbt;
        }

        public static Stats fromNbt(CompoundTag nbt) {
            Map<String, Long> groups = new HashMap<>();
            CompoundTag groupsTag = nbt.getCompound(NBT_GROUP);
            for (String key : groupsTag.getAllKeys()) {
                groups.put(key, groupsTag.getLong(key));
            }

            Map<String, Long> lines = new HashMap<>();
            CompoundTag linesTag = nbt.getCompound(NBT_LINE);
            for (String key : linesTag.getAllKeys()) {
                lines.put(key, linesTag.getLong(key));
            }

            Map<String, Long> names = new HashMap<>();
            CompoundTag namesTag = nbt.getCompound(NBT_NAME);
            for (String key : namesTag.getAllKeys()) {
                names.put(key, namesTag.getLong(key));
            }

            return new Stats(nbt.getLong(NBT_LAST_DEPARTURE), groups, lines, names);
        }
    }

    private static final String NBT_DATA = "DepartureHistory";

    private static final Map<String /* station name */, Data> departuresByStation = new ConcurrentHashMap<>();

    private DepartureHistory() {}

    public static void clear() {
        departuresByStation.clear();
    }

    public static void updateDepartures(String stationName, Train train) {
        departuresByStation.computeIfAbsent(stationName, x -> new Data()).setDeparture(train);
    }

    public static Map<String, Data> getDeparturesAtStation(String stationFilter) {
        return departuresByStation.entrySet().stream()
            .filter(entry -> TrainUtils.stationMatches(entry.getKey(), stationFilter))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ;
    }

    public static long getLatestDepartureFor(ETrainFilter trainFilter, Train train, String stationName) {
        long latestDepartureTime = Long.MIN_VALUE;
        Map<String, Data> dataSrc = getDeparturesAtStation(stationName);
        Optional<TrainData> data = TrainListener.getTrainData(train);
        if (!data.isPresent()) {
            return latestDepartureTime;
        }
        Optional<TrainGroup> group = data.get().getCurrentSection().getTrainGroup();
        Optional<TrainLine> line = data.get().getCurrentSection().getTrainLine();

        for (Map.Entry<String, Data> e : dataSrc.entrySet()) {
            switch (trainFilter) {
                case SAME_GROUP -> latestDepartureTime = Math.max(latestDepartureTime, group.map(x -> e.getValue().getDepartureByGroup(x).orElse(Long.MIN_VALUE)).orElse(Long.MIN_VALUE));
                case SAME_LINE -> latestDepartureTime = Math.max(latestDepartureTime, line.map(x -> e.getValue().getDepartureByLine(x).orElse(Long.MIN_VALUE)).orElse(Long.MIN_VALUE));
                case SAME_NAME -> latestDepartureTime = Math.max(latestDepartureTime, Optional.ofNullable(train.name).map(x -> e.getValue().getDepartureByName(x.getString()).orElse(Long.MIN_VALUE)).orElse(Long.MIN_VALUE));
                default -> latestDepartureTime = Math.max(latestDepartureTime, e.getValue().getLastDepartureTime());
            }
        }
        return latestDepartureTime;
    }

    public static void validate() {
        departuresByStation.keySet().retainAll(TrainUtils.getAllStations().stream().map(x -> x.name).toList());
    }

    public static int debug_dataCount() {
        return departuresByStation.size();
    }



    public static CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        CompoundTag data = new CompoundTag();
        for (Map.Entry<String, Data> e : departuresByStation.entrySet()) {
            data.put(e.getKey(), e.getValue().toNbt());
        }
        nbt.put(NBT_DATA, data);
        return nbt;
    }

    public static void fromNbt(CompoundTag nbt) {
        departuresByStation.clear();
        CompoundTag data = nbt.getCompound(NBT_DATA);
        for (String key : data.getAllKeys()) {
            departuresByStation.put(key, Data.fromNbt(data.getCompound(key)));
        }
    }
}
