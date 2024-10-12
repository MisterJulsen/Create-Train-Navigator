package de.mrjulsen.crn.data.train.portable;

import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Contains data about one train arrival at a specific station. This data is used by displays and does not provide any additional functionality. */
public class NextConnectionsDisplayData {
    private final List<TrainStopDisplayData> stops;

    private static final String NBT_STOPS = "Stops";

    public NextConnectionsDisplayData(
        List<TrainStopDisplayData> stops
    ) {
        this.stops = stops;
    }

    public static NextConnectionsDisplayData empty() {
        return new NextConnectionsDisplayData(List.of());
    }

    /** Server-side only! */
    public static NextConnectionsDisplayData at(String stationName, UUID selfTrainId) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        
        return new NextConnectionsDisplayData(
            TrainUtils.getDeparturesAt(GlobalSettings.getInstance().getOrCreateStationTagFor(TagName.of(stationName)), selfTrainId).stream().map(x -> TrainStopDisplayData.of(x)).toList()
        );
    }
    public List<TrainStopDisplayData> getConnections() {
        return stops;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        ListTag list = new ListTag();
        list.addAll(stops.stream().map(x -> x.toNbt()).toList());        

        nbt.put(NBT_STOPS, list);
        return nbt;
    }

    public static NextConnectionsDisplayData fromNbt(CompoundTag nbt) {
        return new NextConnectionsDisplayData(
            nbt.getList(NBT_STOPS, Tag.TAG_COMPOUND).stream().map(x -> TrainStopDisplayData.fromNbt((CompoundTag)x)).toList()
        );
    }
}
