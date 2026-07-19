package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

/**
 * An immutable snapshot of how a train is made up: its carriages, their sizes and where they are.
 * <p>
 * Kept separate from {@link TrainSnapshot} because it is only of interest to consumers that draw or
 * inspect the physical train - a departure board never needs it.
 *
 * @param trainId      The id of the train.
 * @param carriages    The carriages from front to back.
 * @param totalLengthBlocks How much track the whole train occupies, in blocks.
 * @param doubleEnded  Whether the train can be driven from either end.
 * @param backwards    Whether the train is currently running backwards along its carriage order.
 */
public record TrainCompositionSnapshot(
    UUID trainId,
    List<CarriageSnapshot> carriages,
    int totalLengthBlocks,
    boolean doubleEnded,
    boolean backwards
) {

    public TrainCompositionSnapshot {
        carriages = carriages == null ? List.of() : List.copyOf(carriages);
    }

    /** Captures the composition of the given train. Server thread only. */
    public static TrainCompositionSnapshot of(Train train) {
        List<CarriageSnapshot> carriages = new ArrayList<>();
        int length = 0;

        if (train.carriages != null) {
            for (int i = 0; i < train.carriages.size(); i++) {
                Carriage carriage = train.carriages.get(i);
                if (carriage == null) {
                    continue;
                }
                carriages.add(CarriageSnapshot.of(carriage, i));
                length += carriage.bogeySpacing;
            }
        }
        if (train.carriageSpacing != null) {
            for (Integer spacing : train.carriageSpacing) {
                length += spacing == null ? 0 : spacing;
            }
        }

        return new TrainCompositionSnapshot(train.id, carriages, length, train.doubleEnded, train.currentlyBackwards);
    }

    /** The number of carriages. */
    public int carriageCount() {
        return carriages.size();
    }

    /** Whether any carriage is currently stalled. */
    public boolean hasStalledCarriage() {
        return carriages.stream().anyMatch(CarriageSnapshot::stalled);
    }

    /** Whether any carriage carries cargo storage. */
    public boolean hasStorage() {
        return carriages.stream().anyMatch(CarriageSnapshot::hasStorage);
    }

    /** Whether the train currently spans more than one dimension. */
    public boolean spansDimensions() {
        return carriages.stream().anyMatch(CarriageSnapshot::inMultipleDimensions);
    }
}
