package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class GetAllStationNamesPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";
    private List<String> names;

    public GetAllStationNamesPacketData(DLStatus status) {
        super(status);
    }

    public GetAllStationNamesPacketData(List<String> names) {
        super(DLStatus.OK);
        this.names = names;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (String name : names) {
            list.add(StringTag.valueOf(name));
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.names = nbt.getList(NBT_DATA, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
    }

    public List<String> getStations() {
        return names;
    }
    

    public static GetAllStationNamesPacketData handle(NetworkPacketContext context) {
        return new GetAllStationNamesPacketData(TrainUtils.getAllStations().stream().map(x -> x.name).toList());
    }
}
