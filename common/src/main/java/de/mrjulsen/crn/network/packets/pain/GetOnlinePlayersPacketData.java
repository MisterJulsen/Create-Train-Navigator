package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetOnlinePlayersPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";
    private List<Owner> players;

    public GetOnlinePlayersPacketData(DLStatus status) {
        super(status);
    }

    public GetOnlinePlayersPacketData(List<Owner> players) {
        super(DLStatus.OK);
        this.players = players;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Owner player : players) {
            list.add(player.toNbt());
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.players = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> Owner.fromNbt((CompoundTag)x)).sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
    }
    
    public List<Owner> getPlayers() {
        return players;
    }

    public static GetOnlinePlayersPacketData handle(NetworkPacketContext context) {
        return new GetOnlinePlayersPacketData(CRNPlatformSpecific.getAllKnownPlayers().entrySet().stream().map(e -> new Owner(e.getKey())).toList());
    }
}
