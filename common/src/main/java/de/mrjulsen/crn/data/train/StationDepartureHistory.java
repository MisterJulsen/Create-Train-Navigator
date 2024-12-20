package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.data.MapCache;
import net.minecraft.nbt.CompoundTag;

public final class StationDepartureHistory {

    private StationDepartureHistory() {}

    public static final ConcurrentHashMap<String, Data> trainDepartures = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<DepartureTimeInputDataKey>> departureInputKeyByStation = new ConcurrentHashMap<>();


    public static String debug_departureHistory() {
        long a = trainDepartures.size();
        long b = trainDepartures.values().stream().mapToLong(x -> x.debug_cachedDataCount()).sum();
        long c = departureInputKeyByStation.size();
        long d = departureInputKeyByStation.values().stream().mapToLong(x -> x.size()).sum();
        long e = departureDataCache.getCachedDataCount();
        long f = lastDepartureTimeDataCache.getCachedDataCount();

        return String.format("DH: [%s,%s]/[%s,%s]/[%s,%s]", a, b, c, d, e, f);
    }

    private static record DepartureTimeInputDataKey(ETrainFilter filter, UUID train, String stationName) {
        @Override
        public final int hashCode() {
            return Objects.hash(filter.getIndex(), train, stationName);
        }
        @Override
        public final boolean equals(Object a) {
            if (a instanceof DepartureTimeInputDataKey o) {
                return filter == o.filter && train.equals(o.train) && stationName.equals(o.stationName);
            }
            return false;
        }
    }

    private static final MapCache<List<Data>, String, String> departureDataCache = new MapCache<>((stationName) -> {
        List<Data> departureData = new ArrayList<>();
               
        if (stationName.contains("*")) {
            String regex = stationName.isBlank() ? stationName : "\\Q" + stationName.replace("*", "\\E.*\\Q") + "\\E";
            for (Map.Entry<String, Data> e : trainDepartures.entrySet()) {
                if (!e.getKey().matches(regex)) continue;
                departureData.add(e.getValue());
            }
        } else if (trainDepartures.containsKey(stationName)) {
            departureData.add(trainDepartures.get(stationName));
        }
        
        return departureData;
    }, String::hashCode, ECachingPriority.LOW);

    private static final MapCache<Long, DepartureTimeInputDataKey, DepartureTimeInputDataKey> lastDepartureTimeDataCache = new MapCache<>((key) -> {
        long time = Long.MIN_VALUE;
        TrainTravelSection section = null;
        
        if (TrainListener.data.containsKey(key.train())) {
            section = TrainListener.data.get(key.train()).getCurrentSection();
        }

        List<Data> data = departureDataCache.get(key.stationName(), key.stationName());
        for (Data d : data) {
            long newTime = d.getLastDepartureTime(key.filter(), section);
            time = Math.max(newTime, time);
        }
        
        return time;
    }, DepartureTimeInputDataKey::hashCode, ECachingPriority.LOW);


    public static synchronized long getLastMatchingDepartureTime(ETrainFilter filter, Train train, String stationName) {
        DepartureTimeInputDataKey key = new DepartureTimeInputDataKey(filter, train.id, stationName);
        departureInputKeyByStation.computeIfAbsent(stationName, x -> new HashSet<>()).add(key);
        return lastDepartureTimeDataCache.get(key, key);
    }
    
    public static synchronized List<Data> getAllDeparturesAt(String stationName) {
        return departureDataCache.get(stationName, stationName);
    }

    public static boolean hasDepartureHistory(String stationName) {
        return trainDepartures.containsKey(stationName);
    }

    public static synchronized void updateDepartureHistory(Train train, String stationName) {
        if (train == null || stationName == null || stationName.isEmpty()) return;
        trainDepartures.computeIfAbsent(stationName, x -> new Data()).setDeparture(train);
        if (departureInputKeyByStation.containsKey(stationName)) {
            Set<DepartureTimeInputDataKey> keys = departureInputKeyByStation.remove(stationName);
            for (DepartureTimeInputDataKey key : keys) {
                lastDepartureTimeDataCache.clear(key);
            }
        }
        departureDataCache.clear(stationName);
    }

    public static synchronized void cleanUpDepartureHistory() {
        Collection<GlobalStation> stations = TrainUtils.getAllStations();
        Collection<String> stationNames = new ArrayList<>(stations.size());
        for (GlobalStation s : stations) {
            stationNames.add(s.name);
        }
        if (trainDepartures.keySet().retainAll(stationNames)) {
            lastDepartureTimeDataCache.clearAll();
            departureDataCache.clearAll();
        }
    }

    public static synchronized void clearAll() {
        trainDepartures.clear();
        departureInputKeyByStation.clear();
        departureDataCache.clearAll();
        lastDepartureTimeDataCache.clearAll();
    }



    public static class Data {
        private long lastDepartureTime = Long.MIN_VALUE;
        private Map<TrainLine, Long> lastDepartureByLine = new ConcurrentHashMap<>();
        private Map<TrainGroup, Long> lastDepartureByGroup = new ConcurrentHashMap<>();

        public void setDeparture(Train train) {
            this.lastDepartureTime = DragonLib.getCurrentServer().get().overworld().getGameTime();
            if (TrainListener.data.containsKey(train.id)) {
                TrainData trainData = TrainListener.data.get(train.id);
                TrainTravelSection section = trainData.getCurrentSection();
                section.getTrainLine().ifPresent(x -> this.lastDepartureByLine.put(x, this.lastDepartureTime));
                section.getTrainGroup().ifPresent(x -> this.lastDepartureByGroup.put(x, this.lastDepartureTime));                
            }
        }

        public long getLastDepartureTime(ETrainFilter filter, @Nullable TrainTravelSection section) {
            return switch (filter) {
                case SAME_GROUP -> section != null ? section.getTrainGroup().map(x -> lastDepartureByGroup.getOrDefault(x, Long.MIN_VALUE)).orElse(Long.MIN_VALUE) : Long.MIN_VALUE;
                case SAME_LINE -> section != null ? section.getTrainLine().map(x -> lastDepartureByLine.getOrDefault(x, Long.MIN_VALUE)).orElse(Long.MIN_VALUE) : Long.MIN_VALUE;
                default -> lastDepartureTime;
            };
        }

        public long getLastDepartureTime() {
            return lastDepartureTime;
        }

        public Map<TrainLine, Long> getLastDepartureByLine() {
            return lastDepartureByLine;
        }

        public Map<TrainGroup, Long> getLastDepartureByGroup() {
            return lastDepartureByGroup;
        }

        public long debug_cachedDataCount() {
            return 1 + lastDepartureByLine.size() + lastDepartureByGroup.size();
        }
    }

    public static class StationStats {

        private static final String NBT_NAME = "Name";
        private static final String NBT_DEPARTURE_TIME = "LastDepartureTime";
        private static final String NBT_LINE = "Line";
        private static final String NBT_GROUP = "Group";
        private static final String NBT_LINE_COUNT = "LineCount";
        private static final String NBT_GROUP_COUNT = "GroupCount";

        private final String stationName;
        private final long lastDepartureTime;

        // On server
        private final Map<String, Long> departuresByLine;
        private final Map<String, Long> departuresByGroup;

        // On client
        private final List<Map.Entry<String, Long>> departuresListByLine;
        private final List<Map.Entry<String, Long>> departuresListByGroup;
        private final int departuresByLineTotalCount;
        private final int departuresByGroupTotalCount;

        public StationStats(String stationName) {
            this.stationName = stationName;
            List<StationDepartureHistory.Data> departures = StationDepartureHistory.getAllDeparturesAt(stationName);
            long lastDepartureTime = Long.MIN_VALUE;
            departuresByLine = new HashMap<>();
            departuresByGroup = new HashMap<>();
            for (StationDepartureHistory.Data data : departures) {
                lastDepartureTime = Math.max(lastDepartureTime, data.getLastDepartureTime(ETrainFilter.ANY, null));
                for (Map.Entry<TrainLine, Long> line : data.getLastDepartureByLine().entrySet()) {
                    departuresByLine.merge(line.getKey().getLineName(), line.getValue(), (k, v) -> Math.max(v, line.getValue()));
                }
                for (Map.Entry<TrainGroup, Long> line : data.getLastDepartureByGroup().entrySet()) {
                    departuresByGroup.merge(line.getKey().getGroupName(), line.getValue(), (k, v) -> Math.max(v, line.getValue()));
                }
            }
            this.lastDepartureTime = lastDepartureTime;
            this.departuresListByLine = null;
            this.departuresListByGroup = null;
            departuresByLineTotalCount = 0;
            departuresByGroupTotalCount = 0;
        }

        private StationStats(String stationName, long lastDepartureTime, List<Map.Entry<String, Long>> departuresByLine, List<Map.Entry<String, Long>> departuresByGroup, int linesCount, int groupsCount) {
            this.stationName = stationName;
            this.lastDepartureTime = lastDepartureTime;
            this.departuresListByLine = departuresByLine;
            this.departuresListByGroup = departuresByGroup;
            this.departuresByLine = null;
            this.departuresByGroup = null;
            this.departuresByLineTotalCount = linesCount;
            this.departuresByGroupTotalCount = groupsCount;
        }

        public static StationStats empty() {
            return new StationStats("", -1, null, null, 0, 0);
        }

        public boolean isEmpty() {
            return lastDepartureTime < 0 && !hasDeparturesByLine() && !hasDeparturesByGroup();
        }

        public boolean hasDeparturesByLine() {
            return departuresListByLine != null && !departuresListByLine.isEmpty();
        }

        public boolean hasDeparturesByGroup() {
            return departuresListByGroup != null && !departuresListByGroup.isEmpty();
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_NAME, stationName);
            nbt.putLong(NBT_DEPARTURE_TIME, lastDepartureTime);

            CompoundTag map1 = new CompoundTag();
            int i = 0;
            for (Map.Entry<String, Long> e : departuresByLine.entrySet()) {
                map1.putLong(e.getKey(), e.getValue());
                i++;
                if (i > 5) break;
            }
            nbt.put(NBT_LINE, map1);
            
            CompoundTag map2 = new CompoundTag();
            i = 0;
            for (Map.Entry<String, Long> e : departuresByGroup.entrySet()) {
                map2.putLong(e.getKey(), e.getValue());
                i++;
                if (i > 5) break;
            }
            nbt.put(NBT_GROUP, map2);
            nbt.putInt(NBT_LINE_COUNT, departuresByLine.size());
            nbt.putInt(NBT_GROUP_COUNT, departuresByGroup.size());
            return nbt;
        }

        public static StationStats fromNbt(CompoundTag nbt) {
            Map<String, Long> departuresByLine = new HashMap<>();
            Map<String, Long> departuresByGroup = new HashMap<>();

            CompoundTag map1 = nbt.getCompound(NBT_LINE);
            for (String key : map1.getAllKeys()) {
                departuresByLine.put(key, map1.getLong(key));
            }

            CompoundTag map2 = nbt.getCompound(NBT_GROUP);
            for (String key : map2.getAllKeys()) {
                departuresByGroup.put(key, map2.getLong(key));
            }

            List<Map.Entry<String, Long>> sortedLines = new ArrayList<>(departuresByLine.entrySet());
            sortedLines.sort(Map.Entry.comparingByValue((a, b) -> Long.compare(a, b) * -1));
            List<Map.Entry<String, Long>> sortedGroups = new ArrayList<>(departuresByGroup.entrySet());
            sortedGroups.sort(Map.Entry.comparingByValue((a, b) -> Long.compare(a, b) * -1));

            return new StationStats(
                nbt.getString(NBT_NAME),
                nbt.getLong(NBT_DEPARTURE_TIME),
                sortedLines,
                sortedGroups,
                nbt.getInt(NBT_LINE_COUNT),
                nbt.getInt(NBT_GROUP_COUNT)
            );
        }

        public String getStationName() {
            return stationName;
        }

        public long getLastDepartureTime() {
            return lastDepartureTime;
        }

        public List<Map.Entry<String, Long>> getDeparturesByLine() {
            return departuresListByLine;
        }

        public List<Map.Entry<String, Long>> getDeparturesByGroup() {
            return departuresListByGroup;
        }

        public int getDeparturesByLineTotalCount() {
            return departuresByLineTotalCount;
        }

        public int getDeparturesByGroupTotalCount() {
            return departuresByGroupTotalCount;
        }
    }

    
	public static enum ETrainFilter implements ITranslatableEnum {
		ANY((byte)0, "any"),
		SAME_LINE((byte)1, "same_line"),
		SAME_GROUP((byte)2, "same_group");

		final byte index;
		final String name;

		ETrainFilter(byte index, String name) {
			this.index = index;
			this.name = name;
		}

		public byte getIndex() {
			return index;
		}

		public String getName() {
			return name;
		}

		public static ETrainFilter getByIndex(int index) {
			return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(ANY);
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
}
