package de.mrjulsen.crn.data;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.event.listeners.IJourneyListenerClient;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SimpleRoute implements AutoCloseable {

    private static final String NBT_PARTS = "Parts";
    private static final String NBT_REFRESH_TIME = "RefreshTime";

    protected final long refreshTime;
    protected final List<SimpleRoutePart> parts;

    // Cache
    protected int stationCount = -1;
    protected StationEntry startStation = null;
    protected StationEntry endStation = null;
    protected StationEntry[] stationArray = null;
    protected boolean valid = true;

    // listener
    protected UUID listenerId;

    public SimpleRoute(Route route) {
        this(route.getParts().stream().map(x -> new SimpleRoutePart(x, route.getRefreshTime())).toList(), route.getRefreshTime());
    }

    protected SimpleRoute(List<SimpleRoutePart> parts, long refreshTime) {
        this.parts = new ArrayList<>(parts);
        this.refreshTime = refreshTime;

        parts.forEach(x -> x.setParent(this));
        tagAll();
    }

    public UUID listen(IJourneyListenerClient initialListener) {
        dispose();
        return listenerId = JourneyListenerManager.create(this, initialListener);
    }

    public void dispose() {
        if (listenerId != null) {
            JourneyListenerManager.remove(listenerId);
        }
    }

    public UUID getListenerId() {
        return listenerId;
    }

    public List<SimpleRoutePart> getParts() {
        return parts;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    protected void tagAll() {        
        final int maxIndex = getParts().size() - 1;
        int partIndex = 0;
        int stationIndex = 0;

        for (SimpleRoutePart part : getParts()) {
            final int idx = partIndex;
            final StationTrainDetails trainDetails = new StationTrainDetails(part.getTrainName(), part.getTrainID(), part.getScheduleTitle());

            part.getStartStation().train = trainDetails;
            part.getStartStation().tag = idx <= 0 ? StationTag.START : StationTag.PART_START;
            part.getStartStation().index = stationIndex;
            stationIndex++;

            for (StationEntry station : part.getStopovers()) {
                station.train = trainDetails;
                station.tag = StationTag.TRANSIT;
                station.index = stationIndex;
                stationIndex++;
            }

            part.getEndStation().train = trainDetails;
            part.getEndStation().tag = idx >= maxIndex ? StationTag.END : StationTag.PART_END;
            part.getEndStation().index = stationIndex;

            stationIndex++;
            partIndex++;
        }
    }

    public int getStationCount(boolean countTransfersTwice) {
        return stationCount < 0 ? stationCount = parts.stream().mapToInt(x -> x.getStationCount(false)).sum() + (countTransfersTwice ? parts.size() * 2 : parts.size() + 1) : stationCount;
    }

    public int getTransferCount() {
        return getParts().size() - 1;
    }

    public int getTotalDuration() {
        return getEndStation().getTicks() - getStartStation().getTicks();
    }

    protected void setValid(boolean b) {
        this.valid = valid && b;
    }

    public boolean isValid() {
        return valid;
    }

    public StationEntry getStartStation() {
        return startStation == null ? startStation = getParts().stream().findFirst().get().getStartStation() : startStation;
    }

    public StationEntry getEndStation() {
        return endStation == null ? endStation = getParts().stream().reduce((a, b) -> b).get().getEndStation() : endStation;
    }

    public String getName() {
        return String.format("%s - %s", getStartStation().getStationName(), getEndStation().getStationName());
    }

    public StationEntry[] getStationArray() {
        return stationArray == null ? stationArray = getParts().stream().flatMap(x -> x.getStations().stream()).toArray(StationEntry[]::new) : stationArray;
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
        List<SimpleRoutePart> parts = new ArrayList<>(nbt.getList(NBT_PARTS, Tag.TAG_COMPOUND).stream().map(x -> SimpleRoutePart.fromNbt((CompoundTag)x, refreshTime)).toList());
        SimpleRoute route = new SimpleRoute(parts, refreshTime);
        parts.forEach(x -> x.setParent(route));
        return route;
    }

    @Override
    public void close() throws Exception {
        dispose();
    }
    
    public static class SimpleRoutePart {
        private static final String NBT_TRAIN_NAME = "TrainName";
        private static final String NBT_TRAIN_ID = "TrainId";
        private static final String NBT_TRAIN_ICON_ID = "TrainIconId";
        private static final String NBT_SCHEDULE_TITLE = "ScheduleTitle";
        private static final String NBT_START_STATION = "StartStation";
        private static final String NBT_END_STATION = "EndStation";
        private static final String NBT_STOPOVERS = "Stopovers";

        protected SimpleRoute parent;

        protected final String trainName;
        protected final UUID trainId;
        protected final ResourceLocation trainIconId;
        protected final String scheduleTitle;
        protected final StationEntry start;
        protected final StationEntry end;
        protected final Collection<StationEntry> stopovers;

        // Cache
        protected List<StationEntry> allStations = null;

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

        protected SimpleRoutePart(String trainName, UUID trainId, ResourceLocation trainIconId, String scheduleTitle, StationEntry start, StationEntry end, Collection<StationEntry> stopovers) {
            this.trainName = trainName;
            this.trainId = trainId;
            this.trainIconId = trainIconId;
            this.scheduleTitle = scheduleTitle;
            this.start = start;
            this.end = end;
            this.stopovers = stopovers;

            getStations().forEach(x -> x.setParent(this));
        }

        protected void setParent(SimpleRoute parent) {
            this.parent = parent;
        }

        protected SimpleRoute getParent() {
            return parent;
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

        public List<StationEntry> getStations() {
            if (allStations != null) {
                return allStations;
            }
            allStations = new ArrayList<>(stopovers);
            allStations.add(0, getStartStation());
            allStations.add(getEndStation());
            return allStations;
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

            SimpleRoutePart part = new SimpleRoutePart(trainName, trainId, trainIconId, scheduleTitle, start, end, stopovers);
            start.setParent(part);
            end.setParent(part);
            stopovers.forEach(x -> x.setParent(part));
            return part;
        }
    }

    public static class StationEntry {
        private static final String NBT_NAME = "Name";
        private static final String NBT_TICKS = "Ticks";

        protected final String stationName;
        protected final StationInfo info;
        protected final int ticks;
        protected final long refreshTime;
        
        protected int index;
        protected StationTrainDetails train;
        protected StationTag tag;

        protected boolean departed = false;
        protected boolean willMiss = false;
        protected boolean trainCancelled = false;

        protected int currentTicks;
        protected long currentRefreshTime;
        protected StationInfo updatedStationInfo;
        protected boolean wasUpdated = false;

        protected SimpleRoutePart parent;

        public StationEntry(TrainStop stop, long refreshTime) {
            this(
                stop.getStationAlias().getAliasName().get(),
                stop.getStationAlias().getInfoForStation(stop.getPrediction().getNextStopStation()),
                stop.getPrediction().getTicks(), refreshTime
            );
        }

        protected StationEntry(String stationName, StationInfo info, int ticks, long refreshTime) {
            this.stationName = stationName;
            this.info = info;
            this.ticks = ticks;
            this.refreshTime = refreshTime;
            this.currentTicks = ticks;
            this.currentRefreshTime = refreshTime;
            this.updatedStationInfo = info;
        }

        protected void setParent(SimpleRoutePart parent) {
            this.parent = parent;
        }

        protected SimpleRoutePart getParent() {
            return parent;
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

        public long getCurrentTime() {
            return currentRefreshTime + currentTicks;
        }

        public StationInfo getInfo() {
            return info;
        }

        public boolean stationInfoChanged() {
            return !getInfo().equals(getUpdatedInfo());
        }

        public StationInfo getUpdatedInfo() {
            return updatedStationInfo;
        }

        public void updateRealtimeData(int ticks, long refreshTime, StationInfo info, Runnable onDelayed) {
            this.wasUpdated = true;

            this.currentRefreshTime = refreshTime;
            this.currentTicks = ticks;
            this.updatedStationInfo = info;
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

        public long getEstimatedTimeWithThreshold() {
            return getScheduleTime() + ((long)(getDifferenceTime() / ModClientConfig.REALTIME_PRECISION_THRESHOLD.get()) * ModClientConfig.REALTIME_PRECISION_THRESHOLD.get());
        }

        public StationTrainDetails getTrain() {
            return train;
        }

        public int getIndex() {
            return index;
        }

        public StationTag getTag() {
            return tag;
        }

        public boolean isDelayed() {
            return getEstimatedTime() - ModClientConfig.DEVIATION_THRESHOLD.get() > getScheduleTime();
        }

        

        public void setDeparted(boolean b) {
            this.departed = this.departed || b;
        }

        public void setWillMiss(boolean b) {
            this.willMiss = b;
        }

        public void setTrainCancelled(boolean b) {
            if (b) {
                getParent().getParent().setValid(false);
            }
            this.trainCancelled = b;
        }

        public boolean isDeparted() {
            return this.departed;
        }

        public boolean willMissStop() {
            return willMiss;
        }

        public boolean isTrainCancelled() {
            return trainCancelled;
        }

        /**
         * Returns true if this station is reachable. Returns false if the train at this station already departed.
         * @param safeConnection If true this station will no longer be considered as reachable if it may not be reachable due to a delay. But this doesn't mean that the train already departed.
         * @return
         */
        public boolean reachable(boolean safeConnection) {
            return (!safeConnection || !willMissStop()) && !isDeparted() && !isTrainCancelled();
        }

        public boolean shouldRenderRealtime() {
            return !isDeparted() && !isTrainCancelled() && relatimeWasUpdated() && (getEstimatedTime() + ModClientConfig.TRANSFER_TIME.get() + ModClientConfig.REALTIME_EARLY_ARRIVAL_THRESHOLD.get() > getScheduleTime());
        }

        public boolean relatimeWasUpdated() {
            return wasUpdated;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_NAME, getStationName());
            nbt.putInt(NBT_TICKS, getTicks());
            getInfo().writeNbt(nbt);
            return nbt;
        }

        public static StationEntry fromNbt(CompoundTag nbt, long refreshTime) {
            String stationName = nbt.getString(NBT_NAME);
            int ticks = nbt.getInt(NBT_TICKS);
            StationInfo info = StationInfo.fromNbt(nbt);
            return new StationEntry(stationName, info, ticks, refreshTime);
        }
    }

    public record StationTrainDetails(String trainName, UUID trainId, String scheduleTitle) {}

    public enum StationTag {
        TRANSIT,
        START,
        PART_START,
        PART_END,
        END;
    }
}

