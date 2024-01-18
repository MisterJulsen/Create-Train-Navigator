package de.mrjulsen.crn.data;

import java.util.UUID;

import org.antlr.v4.parse.ANTLRParser.ruleref_return;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SimpleRoute {

    private static final String NBT_PARTS = "Parts";
    private static final String NBT_REFRESH_TIME = "RefreshTime";

    private final long refreshTime;
    private final Collection<SimpleRoutePart> parts;

    public SimpleRoute(Route route) {
        this(route.getParts().stream().map(x -> new SimpleRoutePart(x, route.getRefreshTime())).toList(), route.getRefreshTime());
    }

    private SimpleRoute(Collection<SimpleRoutePart> parts, long refreshTime) {
        this.parts = new ArrayList<>(parts);
        this.refreshTime = refreshTime;
    }

    public Collection<SimpleRoutePart> getParts() {
        return parts;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public TaggedStationEntry[] getRoutePartsTagged() {
        Collection<TaggedStationEntry> stations = new ArrayList<>();
        
        final int[] index = new int[] { 0, 0 };
        final int maxIndex = getParts().size() - 1;
        getParts().forEach(x -> {
            final int idx = index[1];
            StationTrainDetails details = new StationTrainDetails(x.getTrainName(), x.getTrainID(), x.getScheduleTitle());

            stations.add(new TaggedStationEntry(x.getStartStation(), idx <= 0 ? StationTag.START : StationTag.PART_START, index[0], details));
            index[0]++;

            stations.addAll(x.getStopovers().stream().map(y -> {
                TaggedStationEntry e = new TaggedStationEntry(y, StationTag.TRANSIT, index[0], details);
                index[0]++;
                return e;
            }).toList());

            stations.add(new TaggedStationEntry(x.getEndStation(), idx >= maxIndex ? StationTag.END : StationTag.PART_END, index[0], details));
            index[0]++;
            index[1]++;
        });
        return stations.toArray(TaggedStationEntry[]::new);
    }

    public int getStationCount() {
        return parts.stream().mapToInt(x -> x.getStationCount(false)).sum() + parts.size() + 1;
    }

    public int getTransferCount() {
        return getParts().size() - 1;
    }

    public int getTotalDuration() {
        return getEndStation().getTicks() - getStartStation().getTicks();
    }

    public StationEntry getStartStation() {
        return getParts().stream().findFirst().get().getStartStation();
    }

    public StationEntry getEndStation() {
        return getParts().stream().reduce((a, b) -> b).get().getEndStation();
    }

    public String getName() {
        return String.format("%s - %s", getStartStation().getStationName(), getEndStation().getStationName());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_REFRESH_TIME, getRefreshTime());
        ListTag partsTag = new ListTag();
        partsTag.addAll(getParts().stream().map(x -> x.toNbt()).toList());        
        nbt.put(NBT_PARTS, partsTag);
        return nbt;
    }

    public static SimpleRoute fromNbt(CompoundTag nbt) {
        long refreshTime = nbt.getLong(NBT_REFRESH_TIME);
        Collection<SimpleRoutePart> parts = nbt.getList(NBT_PARTS, Tag.TAG_COMPOUND).stream().map(x -> SimpleRoutePart.fromNbt((CompoundTag)x, refreshTime)).toList();
        return new SimpleRoute(parts, refreshTime);
    }
    
    public static class SimpleRoutePart {
        private static final String NBT_TRAIN_NAME = "TrainName";
        private static final String NBT_TRAIN_ID = "TrainId";
        private static final String NBT_TRAIN_ICON_ID = "TrainIconId";
        private static final String NBT_SCHEDULE_TITLE = "ScheduleTitle";
        private static final String NBT_START_STATION = "StartStation";
        private static final String NBT_END_STATION = "EndStation";
        private static final String NBT_STOPOVERS = "Stopovers";

        private final String trainName;
        private final UUID trainId;
        private final ResourceLocation trainIconId;
        private final String scheduleTitle;
        private final StationEntry start;
        private final StationEntry end;
        private final Collection<StationEntry> stopovers;

        public SimpleRoutePart(RoutePart part, long refreshTime) {
            this(
                part.getTrain().name.getString(),
                part.getTrain().id,
                part.getTrain().icon.getId(),
                part.getStartStation().getPrediction().getScheduleTitle(),
                new StationEntry(part.getStartStation(), refreshTime),
                new StationEntry(part.getEndStation(), refreshTime),
                part.getStopovers().stream().map(x -> new StationEntry(x, refreshTime)).toList()
            );
        }

        private SimpleRoutePart(String trainName, UUID trainId, ResourceLocation trainIconId, String scheduleTitle, StationEntry start, StationEntry end, Collection<StationEntry> stopovers) {
            this.trainName = trainName;
            this.trainId = trainId;
            this.trainIconId = trainIconId;
            this.scheduleTitle = scheduleTitle;
            this.start = start;
            this.end = end;
            this.stopovers = stopovers;
        }

        public String getTrainName() {
            return trainName;
        }

        public UUID getTrainID() {
            return trainId;
        }

        public TrainIconType getTrainIcon() {
            return TrainIconType.byId(trainIconId);
        }

        public String getScheduleTitle() {
            return scheduleTitle;
        }

        public StationEntry getStartStation() {
            return start;
        }

        public StationEntry getEndStation() {
            return end;
        }

        public Collection<StationEntry> getStopovers() {
            return stopovers;
        }

        public Collection<StationEntry> getStations() {
            List<StationEntry> stations = new ArrayList<>(stopovers);
            stations.add(0, getStartStation());
            stations.add(getEndStation());
            return stations;
        }

        public int getStationCount(boolean includeStartEnd) {
            return getStopovers().size() + (includeStartEnd ? 2 : 0);
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_TRAIN_NAME, getTrainName());
            nbt.putString(NBT_SCHEDULE_TITLE, getScheduleTitle());
            nbt.putUUID(NBT_TRAIN_ID, getTrainID());
            nbt.putString(NBT_TRAIN_ICON_ID, trainIconId.toString());
            nbt.put(NBT_START_STATION, getStartStation().toNbt());
            nbt.put(NBT_END_STATION, getEndStation().toNbt());
            ListTag stopoversTag = new ListTag();
            stopoversTag.addAll(stopovers.stream().map(x -> x.toNbt()).toList());        
            nbt.put(NBT_STOPOVERS, stopoversTag);
            return nbt;
        }

        public static SimpleRoutePart fromNbt(CompoundTag nbt, long refreshTime) {
            String trainName = nbt.getString(NBT_TRAIN_NAME);
            String scheduleTitle = nbt.getString(NBT_SCHEDULE_TITLE);
            UUID trainId = nbt.getUUID(NBT_TRAIN_ID);
            ResourceLocation trainIconId = new ResourceLocation(nbt.getString(NBT_TRAIN_ICON_ID));
            StationEntry start = StationEntry.fromNbt(nbt.getCompound(NBT_START_STATION), refreshTime);
            StationEntry end = StationEntry.fromNbt(nbt.getCompound(NBT_END_STATION), refreshTime);
            Collection<StationEntry> stopovers = nbt.getList(NBT_STOPOVERS, Tag.TAG_COMPOUND).stream().map(x -> StationEntry.fromNbt((CompoundTag)x, refreshTime)).toList();

            return new SimpleRoutePart(trainName, trainId, trainIconId, scheduleTitle, start, end, stopovers);
        }
    }

    public static class StationEntry {
        private static final String NBT_NAME = "Name";
        private static final String NBT_TICKS = "Ticks";

        private static final int TRESHOLD = 167;

        private final String stationName;
        private final int ticks;
        private final long refreshTime;

        private int currentTicks;
        private long currentRefreshTime;

        public StationEntry(TrainStop stop, long refreshTime) {
            this(stop.getStationAlias().getAliasName().get(), stop.getPrediction().getTicks(), refreshTime);
        }

        private StationEntry(String stationName, int ticks, long refreshTime) {
            this.stationName = stationName;
            this.ticks = ticks;
            this.refreshTime = refreshTime;
            this.currentTicks = ticks;
            this.currentRefreshTime = refreshTime;
        }

        public String getStationName() {
            return stationName;
        }

        public int getTicks() {
            return ticks;
        }

        public long getRefreshTime() {
            return refreshTime;
        }        

        public int getCurrentTicks() {
            return currentTicks;
        }

        public long getCurrentRefreshTime() {
            return currentRefreshTime;
        }

        public void updateRealtimeData(int ticks, long refreshTime) {
            this.currentRefreshTime = refreshTime;
            this.currentTicks = ticks;
        }

        public long getScheduleTime() {
            return getRefreshTime() + getTicks();
        }

        public long getEstimatedTime() {
            return getCurrentRefreshTime() + getCurrentTicks();
        }

        public long getDifferenceTime() {
            return getEstimatedTime() - getScheduleTime();
        }

        public long getEstimatedTimeWithTreshold() {
            
            return getCurrentRefreshTime() + getCurrentTicks();
            //return getScheduleTime() + ((long)(getDifferenceTime() / TRESHOLD) * TRESHOLD);
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_NAME, getStationName());
            nbt.putInt(NBT_TICKS, getTicks());
            return nbt;
        }

        public static StationEntry fromNbt(CompoundTag nbt, long refreshTime) {
            String stationName = nbt.getString(NBT_NAME);
            int ticks = nbt.getInt(NBT_TICKS);
            return new StationEntry(stationName, ticks, refreshTime);
        }
    }

    public record StationTrainDetails(String trainName, UUID trainId, String scheduleTitle) {}
    public record TaggedStationEntry(StationEntry station, StationTag tag, int index, StationTrainDetails train) {}

    public enum StationTag {
        TRANSIT,
        START,        
        PART_START,
        PART_END,
        END;
    }
}

