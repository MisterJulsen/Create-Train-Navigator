package de.mrjulsen.crn.network.packets.pain;

import java.util.Collection;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetAllStationTagsPacketData extends NetworkPacketData {
    
    private static final String NBT_DATA = "Data";

    private Collection<StationTag> tags;

    public GetAllStationTagsPacketData(DLStatus status) {
        super(status);
    }
    
    public GetAllStationTagsPacketData(Collection<StationTag> tags) {
        super(DLStatus.OK);
        this.tags = tags;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (StationTag tag : tags) {
            list.add(tag.toNbt());
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.tags = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> StationTag.fromNbt((CompoundTag)x, null)).toList();
    }

    public Collection<StationTag> getTags() {
        return tags;
    }

    public static GetAllStationTagsPacketData handle(NetworkPacketContext context) {
        return new GetAllStationTagsPacketData(GlobalSettings.getInstance().getAllStationTags().stream().sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList());
    }
    
}
