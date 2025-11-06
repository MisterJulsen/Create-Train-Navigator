package de.mrjulsen.crn.network.packets.pain;

import java.util.Collection;
import java.util.List;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class AddTrainToBlacklistPacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private String name;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(String name) {
            super(DLStatus.OK);
            this.name = name;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString(NBT_DATA, name);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = nbt.getString(NBT_DATA);
        }
    }

    public static class Response extends NetworkPacketData {
        private Collection<String> names;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Collection<String> names) {
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

        public Collection<String> getNames() {
            return names;
        }

        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        if (!GlobalSettings.modificationsAllowed(context.getPlayer())) {
            return new Response(List.of());
        }        
        GlobalSettings.getInstance().blacklistTrain(packet.name);
        return new Response(GlobalSettings.getInstance().getAllBlacklistedTrains());
    }
    
}
