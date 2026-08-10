package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public record CreateTrainSnapshot(
        @ResponseAlwaysInclude UUID id,
        String name,
        UUID owner,
        UUID graph,
        ResourceLocation dimension,
        Vec3Snapshot position,
        double speed,
        boolean backwards,
        boolean derailed,
        CreateTrainStatusSnapshot status,
        List<CreateCarriageSnapshot> carriages,
        UUID currentStation
) {

    public static CreateTrainSnapshot of(Train train) {
        GlobalStation currentStation = train.getCurrentStation();
        return new CreateTrainSnapshot(
                train.id,
                train.name.getString(),
                train.owner,
                train.graph == null ? null : train.graph.id,
                dimensionOf(train),
                Vec3Snapshot.of(worldPositionOf(train)),
                train.speed,
                train.currentlyBackwards,
                train.derailed,
                CreateTrainStatusSnapshot.of(train),
                train.carriages.stream().map(carriage -> CreateCarriageSnapshot.of(carriage, train.graph)).toList(),
                currentStation == null ? null : currentStation.id
        );
    }

    private static TravellingPoint leadingPointOf(Train train) {
        if (train.graph == null || train.carriages.isEmpty()) {
            return null;
        }
        return train.carriages.get(0).getLeadingPoint();
    }

    private static Vec3 worldPositionOf(Train train) {
        TravellingPoint point = leadingPointOf(train);
        if (point == null || point.edge == null) {
            return null;
        }
        return point.getPosition(train.graph);
    }

    public static ResourceLocation dimensionOf(Train train) {
        TravellingPoint point = leadingPointOf(train);
        if (point == null || point.node1 == null) {
            return null;
        }
        return point.node1.getLocation().getDimension().location();
    }
}
