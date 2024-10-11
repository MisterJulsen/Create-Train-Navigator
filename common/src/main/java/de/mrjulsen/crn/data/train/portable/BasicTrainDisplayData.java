package de.mrjulsen.crn.data.train.portable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Contains data about one train arrival at a specific station. This data is used by displays and does not provide any additional functionality. */
public class BasicTrainDisplayData {
    private final UUID id;
    private final String name;
    private final TrainIconType icon;
    private final List<CompiledTrainStatus> status;
    private final boolean cancelled;

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_ICON = "Icon";
    private static final String NBT_STATUS = "Status";
    private static final String NBT_CANCELLED = "Cancelled";

    public BasicTrainDisplayData(
        UUID id,
        String name,
        TrainIconType icon,
        List<CompiledTrainStatus> status,
        boolean cancelled
    ) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.status = status;
        this.cancelled = cancelled;
    }

    public static BasicTrainDisplayData empty() {
        return new BasicTrainDisplayData(new UUID(0, 0), "", TrainIconType.getDefault(), List.of(), true);
    }

    /** Server-side only! */
    public static BasicTrainDisplayData of(UUID train) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        if (!TrainListener.data.containsKey(train)) {
            return empty();
        }
        TrainData data = TrainListener.data.get(train);
        return new BasicTrainDisplayData(
            data.getTrainId(),
            data.getTrainName(),
            TrainIconType.getDefault(), // TODO
            new ArrayList<>(data.getStatus()),
            data.isCancelled()
        );
    }

    public static BasicTrainDisplayData of(TrainStop stop) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        if (!TrainListener.data.containsKey(stop.getTrainId())) {
            return empty();
        }
        TrainData data = TrainListener.data.get(stop.getTrainId());
        return new BasicTrainDisplayData(
            stop.getTrainId(),
            stop.getTrainName(),
            stop.getTrainIcon(),
            new ArrayList<>(data.getStatus()),
            data.isCancelled()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TrainIconType getIcon() {
        return icon;
    }

    public List<CompiledTrainStatus> getStatus() {
        return status;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean hasStatusInfo() {
        return !getStatus().isEmpty();
    }
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag statusList = new ListTag();
        statusList.addAll(status.stream().map(x -> x.toNbt()).toList());

        nbt.putUUID(NBT_ID, id);
        nbt.putString(NBT_NAME, name);
        nbt.putString(NBT_ICON, icon.getId().toString());
        nbt.put(NBT_STATUS, statusList);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        return nbt;
    }

    public static BasicTrainDisplayData fromNbt(CompoundTag nbt) {
        return new BasicTrainDisplayData(
            nbt.getUUID(NBT_ID),
            nbt.getString(NBT_NAME), 
            TrainIconType.byId(new ResourceLocation(nbt.getString(NBT_ICON))),
            nbt.getList(NBT_STATUS, Tag.TAG_COMPOUND).stream().map(x -> CompiledTrainStatus.fromNbt(((CompoundTag)x))).toList(),
            nbt.getBoolean(NBT_CANCELLED)
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
