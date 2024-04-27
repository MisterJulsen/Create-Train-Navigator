package de.mrjulsen.crn.data;

import java.util.Optional;

import com.simibubi.create.content.trains.station.GlobalStation;

import net.minecraft.network.FriendlyByteBuf;

public class NearestTrackStationResult {
    public final double distance;
    public final Optional<TrainStationAlias> aliasName;

    public NearestTrackStationResult(Optional<GlobalStation> station, double distance) {
       this(station.isPresent() ? GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(station.get().name) : null, distance);
    }

    private NearestTrackStationResult(TrainStationAlias station, double distance) {
        this.distance = distance;
        this.aliasName = station != null ? Optional.of(station) : Optional.empty();
    }

    public static NearestTrackStationResult empty() {
        return new NearestTrackStationResult(Optional.empty(), 0);
    }

    public void serialize(FriendlyByteBuf buffer) {
        buffer.writeBoolean(aliasName.isPresent());
        if (aliasName.isPresent()) {
            buffer.writeNbt(aliasName.get().toNbt());
        }
        buffer.writeDouble(distance);
    }

    public static NearestTrackStationResult deserialize(FriendlyByteBuf buffer) {
        boolean valid = buffer.readBoolean();
        TrainStationAlias alias = null;
        if (valid) {
            alias = TrainStationAlias.fromNbt(buffer.readNbt());
        }
        double distance = buffer.readDouble();
        return new NearestTrackStationResult(alias, distance);
    }
}
