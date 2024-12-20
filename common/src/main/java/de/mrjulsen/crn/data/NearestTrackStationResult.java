package de.mrjulsen.crn.data;

import java.util.Optional;

import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import net.minecraft.nbt.CompoundTag;

public class NearestTrackStationResult {

    public static final String NBT_DISTANCE = "Distance";
    public static final String NBT_TAG = "TagName";


    public final double distance;
    public final Optional<TagName> tagName;

    public NearestTrackStationResult(Optional<GlobalStation> station, double distance) {
       this(station.isPresent() ? GlobalSettings.getInstance().getOrCreateStationTagFor(station.get().name).getTagName() : null, distance);
    }

    private NearestTrackStationResult(TagName tagName, double distance) {
        this.distance = distance;
        this.tagName = Optional.ofNullable(tagName);
    }

    public static NearestTrackStationResult empty() {
        return new NearestTrackStationResult(Optional.empty(), 0);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble(NBT_DISTANCE, distance);
        tagName.ifPresent(x -> nbt.putString(NBT_TAG, tagName.get().get()));
        return nbt;
    }

    public static NearestTrackStationResult fromNbt(CompoundTag nbt) {
        return new NearestTrackStationResult(
            nbt.contains(NBT_TAG) ? TagName.of(nbt.getString(NBT_TAG)) : null,
            nbt.getDouble(NBT_DISTANCE)
        );
    }
}
