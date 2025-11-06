package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class GetAllBlacklistedStationsPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";
    private List<String> names;

    public GetAllBlacklistedStationsPacketData(DLStatus status) {
        super(status);
    }

    public GetAllBlacklistedStationsPacketData(List<String> names) {
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

    public List<String> getNames() {
        return names;
    }

    public static GetAllBlacklistedStationsPacketData handle(NetworkPacketContext context) {
        return new GetAllBlacklistedStationsPacketData(GlobalSettings.getInstance().getAllBlacklistedStations());
    }
}
