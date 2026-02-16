package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class StationTagRequestByTagPacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        private TagName name;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(TagName name) {
            super(DLStatus.OK);
            this.name = name;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, name.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = TagName.fromNbt(nbt.getCompound(NBT_DATA));
        }
    }

    public static class Response extends NetworkPacketData {
        private StationTag tag;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(StationTag tag) {
            super(DLStatus.OK);
            this.tag = tag;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, tag.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.tag = StationTag.fromNbt(nbt.getCompound(NBT_DATA), null);
        }

        public StationTag getTag() {
            return tag;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(GlobalSettings.getInstance().getOrCreateStationTagFor(packet.name));
    }
    
}
