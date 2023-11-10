package de.mrjulsen.crn.data;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.TrainIconType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;

public class SimpleRoute {

    private static final String NBT_PARTS = "Parts";

    private final Collection<SimpleRoutePart> parts;

    public SimpleRoute(Route route) {
        this(route.getParts().stream().map(x -> new SimpleRoutePart(x)).toList());
    }

    private SimpleRoute(Collection<SimpleRoutePart> parts) {
        this.parts = new ArrayList<>(parts);
    }

    public Collection<SimpleRoutePart> getParts() {
        return parts;
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
        ListTag partsTag = new ListTag();
        partsTag.addAll(getParts().stream().map(x -> x.toNbt()).toList());        
        nbt.put(NBT_PARTS, partsTag);
        return nbt;
    }

    public static SimpleRoute fromNbt(CompoundTag nbt) {
        Collection<SimpleRoutePart> parts = nbt.getList(NBT_PARTS, Tag.TAG_COMPOUND).stream().map(x -> SimpleRoutePart.fromNbt((CompoundTag)x)).toList();
        return new SimpleRoute(parts);
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

        public SimpleRoutePart(RoutePart part) {
            this(
                part.getTrain().name.getString(),
                part.getTrain().id,
                part.getTrain().icon.getId(),
                part.getStartStation().getPrediction().getScheduleTitle(),
                new StationEntry(part.getStartStation()),
                new StationEntry(part.getEndStation()),
                part.getStopovers().stream().map(x -> new StationEntry(x)).toList()
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

        public static SimpleRoutePart fromNbt(CompoundTag nbt) {
            String trainName = nbt.getString(NBT_TRAIN_NAME);
            String scheduleTitle = nbt.getString(NBT_SCHEDULE_TITLE);
            UUID trainId = nbt.getUUID(NBT_TRAIN_ID);
            ResourceLocation trainIconId = new ResourceLocation(nbt.getString(NBT_TRAIN_ICON_ID));
            StationEntry start = StationEntry.fromNbt(nbt.getCompound(NBT_START_STATION));
            StationEntry end = StationEntry.fromNbt(nbt.getCompound(NBT_END_STATION));
            Collection<StationEntry> stopovers = nbt.getList(NBT_STOPOVERS, Tag.TAG_COMPOUND).stream().map(x -> StationEntry.fromNbt((CompoundTag)x)).toList();

            return new SimpleRoutePart(trainName, trainId, trainIconId, scheduleTitle, start, end, stopovers);
        }
    }

    public static class StationEntry {
        private static final String NBT_NAME = "Name";
        private static final String NBT_TICKS = "Ticks";

        private final String stationName;
        private final int ticks;

        public StationEntry(TrainStop stop) {
            this(stop.getStationAlias().getAliasName().get(), stop.getPrediction().getTicks());
        }

        private StationEntry(String stationName, int ticks) {
            this.stationName = stationName;
            this.ticks = ticks;
        }

        public String getStationName() {
            return stationName;
        }

        public int getTicks() {
            return ticks;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_NAME, getStationName());
            nbt.putInt(NBT_TICKS, getTicks());
            return nbt;
        }

        public static StationEntry fromNbt(CompoundTag nbt) {
            String stationName = nbt.getString(NBT_NAME);
            int ticks = nbt.getInt(NBT_TICKS);
            return new StationEntry(stationName, ticks);
        }
    }
}

