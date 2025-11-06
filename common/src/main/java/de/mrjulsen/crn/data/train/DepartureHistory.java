package de.mrjulsen.crn.data.train;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.Pair;
import net.minecraft.nbt.CompoundTag;

public final class DepartureHistory {   

    public static class Data {

        private static final String NBT_LAST_DEPARTURE = "LastDeparture";
        private static final String NBT_LINES = "Lines";
        private static final String NBT_CATEGORIES = "Categories";
        private static final String NBT_NAMES = "Names";

        private long lastDepartureTime = Long.MIN_VALUE;
        private Map<TrainLine, Long> lastDepartureByLine = new ConcurrentHashMap<>();
        private Map<TrainCategory, Long> lastDepartureByCategory = new ConcurrentHashMap<>();
        private Map<String, Long> lastDepartureByTrainName = new ConcurrentHashMap<>();

        public void setDeparture(Train train) {
            this.lastDepartureTime = DragonLib.getCurrentServer().get().overworld().getGameTime();
            this.lastDepartureByTrainName.put(train.name.getString(), this.lastDepartureTime);
            TrainListener.getTrainData(train.id).ifPresent(data -> {
                ScheduleSection section = data.getCurrentSection();
                section.getTrainLine().ifPresent(x -> this.lastDepartureByLine.put(x, this.lastDepartureTime));
                section.getTrainCategory().ifPresent(x -> this.lastDepartureByCategory.put(x, this.lastDepartureTime));
            });
        }

        public long getLastDepartureTime(ETrainFilter filter, String trainName, ScheduleSection section) {
            return switch (filter) {
                case SAME_CATEGORY -> section != null ? section.getTrainCategory().map(x -> lastDepartureByCategory.getOrDefault(x, Long.MIN_VALUE)).orElse(Long.MIN_VALUE) : Long.MIN_VALUE;
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

        public Map<TrainCategory, Long> getLastDeparturesByCategory() {
            return lastDepartureByCategory;
        }

        public Map<String, Long> getLastDeparturesByTrainName() {
            return lastDepartureByTrainName;
        }

        public Optional<Long> getDepartureByCategory(TrainCategory category) {
            return Optional.ofNullable(lastDepartureByCategory.containsKey(category) ? lastDepartureByCategory.get(category) : null);
        }

        public Optional<Long> getDepartureByLine(TrainLine line) {
            return Optional.ofNullable(lastDepartureByLine.containsKey(line) ? lastDepartureByLine.get(line) : null);
        }

        public Optional<Long> getDepartureByName(String name) {
            return Optional.ofNullable(lastDepartureByTrainName.containsKey(name) ? lastDepartureByTrainName.get(name) : null);
        }

        public long debug_cachedDataCount() {
            return 1 + lastDepartureByLine.size() + lastDepartureByCategory.size();
        }
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong(NBT_LAST_DEPARTURE, lastDepartureTime);
            
            CompoundTag linesList = new CompoundTag();
            for (Map.Entry<TrainLine, Long> e : lastDepartureByLine.entrySet()) {
                linesList.putLong(e.getKey().getId().toString(), e.getValue());
            }
            nbt.put(NBT_LINES, linesList);
            
            CompoundTag categoriesList = new CompoundTag();
            for (Map.Entry<TrainCategory, Long> e : lastDepartureByCategory.entrySet()) {
                categoriesList.putLong(e.getKey().getId().toString(), e.getValue());
            }
            nbt.put(NBT_CATEGORIES, categoriesList);
            
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
                GlobalSettings.getInstance().getTrainLine(UUID.fromString(key)).ifPresent(x -> data.lastDepartureByLine.put(x, linesList.getLong(key)));
            }
            CompoundTag categoriesList = nbt.getCompound(NBT_CATEGORIES);
            for (String key : categoriesList.getAllKeys()) {
                GlobalSettings.getInstance().getTrainCategory(UUID.fromString(key)).ifPresent(x -> data.lastDepartureByCategory.put(x, categoriesList.getLong(key)));
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
        SAME_CATEGORY((byte)2, "same_category"),
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
        public Data getTranslationData() {
            return new Data(CreateRailwaysNavigator.MOD_ID, "train_filter", name);
        }
    }

    public static class Stats {

        private static final String NBT_LAST_DEPARTURE = "LastDeparture";
        private static final String NBT_LINE = "Line";
        private static final String NBT_CATEGORY = "Category";
        private static final String NBT_NAME = "Name";

        private final long lastDeparture;
        private final Map<String, Long> departuresByCategory;
        private final Map<String, Long> departuresByLine;
        private final Map<String, Long> departuresByName;

        private final Optional<Pair<String, Long>> latestCategoryDeparture;
        private final Optional<Pair<String, Long>> latestLineDeparture;
        private final Optional<Pair<String, Long>> latestNameDeparture;

        public Stats(long lastDeparture, Map<String, Long> departuresByCategory, Map<String, Long> departuresByLine, Map<String, Long> departuresByName) {
            this.lastDeparture = lastDeparture;
            this.departuresByCategory = departuresByCategory;
            this.departuresByLine = departuresByLine;
            this.departuresByName = departuresByName;

            this.latestCategoryDeparture = departuresByCategory.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
            this.latestLineDeparture = departuresByLine.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
            this.latestNameDeparture = departuresByName.entrySet().stream().max((a, b) -> Long.compare(a.getValue(), b.getValue())).map(p -> new Pair<>(p.getKey(), p.getValue()));
        }

        public static Stats of(Data data) {
            Map<String, Long> categories = data.getLastDeparturesByCategory().entrySet().stream().collect(Collectors.toMap(p -> p.getKey().getCategoryName(), p -> p.getValue()));
            Map<String, Long> lines = data.getLastDeparturesByLine().entrySet().stream().collect(Collectors.toMap(p -> p.getKey().getLineName(), p -> p.getValue()));
            Map<String, Long> names = data.getLastDeparturesByTrainName().entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            return new Stats(data.getLastDepartureTime(), categories, lines, names);
        }

        public static Stats ofStation(String stationName) {
            return getDeparturesAtStation(stationName).values().stream().map(Stats::of).findFirst().orElse(Stats.empty());
        }        

        public static Stats empty() {
            return new Stats(-1, Map.of(), Map.of(), Map.of());
        }

        public boolean isEmpty() {
            return getLastDeparture() < 0 && getDeparturesByCategory().isEmpty() && getDeparturesByLine().isEmpty() && getDeparturesByName().isEmpty();
        }

        public long getLastDeparture() {
            return lastDeparture;
        }

        public Map<String, Long> getDeparturesByCategory() {
            return departuresByCategory;
        }

        public Map<String, Long> getDeparturesByLine() {
            return departuresByLine;
        }

        public Map<String, Long> getDeparturesByName() {
            return departuresByName;
        }

        public Optional<Pair<String, Long>> getLatestCategoryDeparture() {
            return latestCategoryDeparture;
        }

        public Optional<Pair<String, Long>> getLatestLineDeparture() {
            return latestLineDeparture;
        }

        public Optional<Pair<String, Long>> getLatestNameDeparture() {
            return latestNameDeparture;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            
            CompoundTag categoriesTag = new CompoundTag();
            for (Map.Entry<String, Long> e : departuresByCategory.entrySet()) {
                categoriesTag.putLong(e.getKey(), e.getValue());
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
            nbt.put(NBT_CATEGORY, categoriesTag);
            nbt.put(NBT_LINE, linesTag);
            nbt.put(NBT_NAME, namesTag);
            return nbt;
        }

        public static Stats fromNbt(CompoundTag nbt) {
            Map<String, Long> categories = new HashMap<>();
            CompoundTag categoriesTag = nbt.getCompound(NBT_CATEGORY);
            for (String key : categoriesTag.getAllKeys()) {
                categories.put(key, categoriesTag.getLong(key));
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

            return new Stats(nbt.getLong(NBT_LAST_DEPARTURE), categories, lines, names);
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
        Optional<TrainCategory> category = data.get().getCurrentSection().getTrainCategory();
        Optional<TrainLine> line = data.get().getCurrentSection().getTrainLine();

        for (Map.Entry<String, Data> e : dataSrc.entrySet()) {
            switch (trainFilter) {
                case SAME_CATEGORY -> latestDepartureTime = Math.max(latestDepartureTime, category.map(x -> e.getValue().getDepartureByCategory(x).orElse(Long.MIN_VALUE)).orElse(Long.MIN_VALUE));
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
