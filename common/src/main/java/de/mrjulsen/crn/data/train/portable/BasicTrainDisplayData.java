package de.mrjulsen.crn.data.train.portable;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Contains data about one train arrival at a specific station. This data is used by displays and does not provide any additional functionality. */
public class BasicTrainDisplayData {

    private record StateData(String displayName, UUID lineId, UUID categoryId, DLColor color) {

        public static final StateData EMPTY = new StateData("", Constants.ZERO_UUID, Constants.ZERO_UUID, DLColor.TRANSPARENT);

        private static final String NBT_DISPLAY_NAME = "DisplayName";
        private static final String NBT_LINE_ID = "LineId";
        private static final String NBT_CATEGORY_ID = "CategoryId";
        private static final String NBT_COLOR = "Color";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_DISPLAY_NAME, displayName);
            nbt.putUUID(NBT_LINE_ID, lineId);
            nbt.putUUID(NBT_CATEGORY_ID, categoryId);
            nbt.putInt(NBT_COLOR, color.getAsARGB());
            return nbt;
        }

        public static StateData fromNbt(CompoundTag nbt) {
            return new StateData(
                    nbt.getString(NBT_DISPLAY_NAME),
                    nbt.getUUID(NBT_LINE_ID),
                    nbt.getUUID(NBT_CATEGORY_ID),
                    DLColor.fromInt(nbt.getInt(NBT_COLOR))
            );
        }
    }

    private final UUID id;
    private final TrainIconType icon;
    private final Collection<ResourceLocation> statusLocations; // Server
    private final boolean cancelled;
    private final Map<ETrainStopState, StateData> dataByState;
    private final int carriages;

    private final Cache<List<CompiledTrainStatus>> clientStatus;
    private static final Cache<Map<ETrainStopState, StateData>> fallbackStateData = new Cache<>(() -> {
        Map<ETrainStopState, StateData> dataByState = new HashMap<>();
        for (ETrainStopState state : ETrainStopState.values()) {
            dataByState.put(state, StateData.EMPTY);
        }
        return ImmutableMap.copyOf(dataByState);
    });

    private static final String NBT_ID = "Id";
    private static final String NBT_ICON = "Icon";
    private static final String NBT_STATUS = "Status";
    private static final String NBT_CANCELLED = "Cancelled";
    private static final String NBT_STATE_DATA = "StateData";
    private static final String NBT_CARRIAGES = "Carriages";

    private BasicTrainDisplayData(
        UUID id,
        TrainIconType icon,
        Collection<ResourceLocation> statusLocations,
        boolean cancelled,
        Map<ETrainStopState, StateData> dataByState,
        int carriages
    ) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(icon, "icon cannot be null");
        Objects.requireNonNull(statusLocations, "statusLocations cannot be null");
        Objects.requireNonNull(dataByState, "dataByState cannot be null");
        this.id = id;
        this.icon = icon;
        this.statusLocations = statusLocations;
        this.cancelled = cancelled;
        this.dataByState = dataByState;
        this.carriages = carriages;

        this.clientStatus = new Cache<>(() -> CompiledTrainStatus.load(statusLocations));
    }

    public static BasicTrainDisplayData empty() {
        return new BasicTrainDisplayData(new UUID(0, 0), /*"", DLColor.TRANSPARENT, */TrainIconType.getDefault(), List.of(), true, fallbackStateData.get(), 0);
    }

    /** Server-side only! */
    public static BasicTrainDisplayData of(UUID train) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }

        return TrainListener.getTrainData(train).map(data -> {
            /*
            final ScheduleSection section = data.getCurrentSection();
            final ScheduleSection prevSection = section.previousSection();
            ScheduleSection selectedSection = section;

            boolean isAtStation = data.waitingAtStationIndex == data.getCurrentScheduleIndex();
            boolean isFirstStationInSection = section.getFirstStop().map(x -> x.getEntryIndex() == data.getCurrentScheduleIndex()).orElse(false);

            if (isFirstStationInSection && !isAtStation && prevSection.shouldIncludeNextStationOfNextSection()) {
                selectedSection = prevSection;
            }
             */
            Map<ETrainStopState, StateData> dataByState = new HashMap<>(ETrainStopState.values().length);
            for (ETrainStopState state : ETrainStopState.values()) {
                ScheduleSection section = state.resolveSection(data.getCurrentSection(), data.waitingAtStationTime, data.getCurrentScheduleIndex());
                StateData stateData = new StateData(
                        data.resolveTrainDisplayName(section),
                        section.getTrainLine().map(TrainLine::getId).orElse(Constants.ZERO_UUID),
                        section.getTrainCategory().map(TrainCategory::getId).orElse(Constants.ZERO_UUID),
                        section.getTrainLine().map(TrainLine::getColor).orElse(DLColor.TRANSPARENT)
                );
                dataByState.put(state, stateData);
            }

            return new BasicTrainDisplayData(
                    data.getTrainId(),
                    data.getTrain().icon,
                    new ArrayList<>(data.getStatus()),
                    data.isCancelled(),
                    dataByState,
                    data.getTrain().carriages.size()
            );
        }).orElse(empty());
    }

    /** Server-side only! */
    public static BasicTrainDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }

        return TrainListener.getTrainData(stop.getTrainId()).map(data -> {
            /*
            final ScheduleSection section = data.getCurrentSection();
            final ScheduleSection prevSection = section.previousSection();
            ScheduleSection selectedSection = section;

            boolean isAtStation = data.waitingAtStationIndex == data.getCurrentScheduleIndex();
            boolean isFirstStationInSection = section.getFirstStop().map(x -> x.getEntryIndex() == data.getCurrentScheduleIndex()).orElse(false);

            if (isFirstStationInSection && !isAtStation && prevSection.shouldIncludeNextStationOfNextSection()) {
                selectedSection = prevSection;
            }

             */
            Map<ETrainStopState, StateData> dataByState = new HashMap<>(ETrainStopState.values().length);
            for (ETrainStopState state : ETrainStopState.values()) {
                ScheduleSection section = state.resolveSection(data.getSectionForIndex(stop.getScheduleIndex()), data.waitingAtStationIndex, stop.getScheduleIndex());
                StateData stateData = new StateData(
                        data.resolveTrainDisplayName(section),
                        section.getTrainLine().map(TrainLine::getId).orElse(Constants.ZERO_UUID),
                        section.getTrainCategory().map(TrainCategory::getId).orElse(Constants.ZERO_UUID),
                        section.getTrainLine().map(TrainLine::getColor).orElse(DLColor.TRANSPARENT)
                );
                dataByState.put(state, stateData);
            }

            return new BasicTrainDisplayData(
                stop.getTrainId(),
                stop.getTrainIcon(),
                new ArrayList<>(data.getStatus()),
                data.isCancelled(),
                dataByState,
                stop.getTrainCarriages()
            );
        }).orElse(empty());
    }

    public UUID getId() {
        return id;
    }

    public String getName(ETrainStopState state) {
        return dataByState.getOrDefault(state, StateData.EMPTY).displayName();
    }

    public TrainIconType getIcon() {
        return icon;
    }

    public List<CompiledTrainStatus> getStatus() {
        return clientStatus.get();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public DLColor getColor(ETrainStopState state) {
        return dataByState.getOrDefault(state, StateData.EMPTY).color();
    }

    public boolean hasColor(ETrainStopState state) {
        return !getColor(state).isTransparent();
    }

    public boolean hasStatusInfo() {
        return !getStatus().isEmpty();
    }

    public int getCarriages() {
        return carriages;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag statusList = new ListTag();
        for (ResourceLocation s : statusLocations) {
            statusList.add(StringTag.valueOf(s.toString()));
        }

        nbt.putUUID(NBT_ID, id);
        nbt.putString(NBT_ICON, icon.getId().toString());
        nbt.put(NBT_STATUS, statusList);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        ModUtils.putMap(nbt, NBT_STATE_DATA, dataByState, (k) -> String.valueOf(k.getId()), StateData::toNbt);
        nbt.putInt(NBT_CARRIAGES, carriages);

        return nbt;
    }

    public static BasicTrainDisplayData fromNbt(CompoundTag nbt) {
        return new BasicTrainDisplayData(
            nbt.getUUID(NBT_ID),
            TrainIconType.byId(new ResourceLocation(nbt.getString(NBT_ICON))),
            nbt.getList(NBT_STATUS, Tag.TAG_STRING).stream().map(x -> new ResourceLocation(((StringTag)x).getAsString())).toList(),
            nbt.getBoolean(NBT_CANCELLED),
            nbt.contains(NBT_STATE_DATA) ? ModUtils.getMap(nbt, NBT_STATE_DATA, (k) -> ETrainStopState.getById(Integer.parseInt(k)), StateData::fromNbt) : fallbackStateData.get(),
            nbt.getInt(NBT_CARRIAGES)
        );
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof BasicTrainDisplayData o && o.getId().equals(getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getId());
    }
}
