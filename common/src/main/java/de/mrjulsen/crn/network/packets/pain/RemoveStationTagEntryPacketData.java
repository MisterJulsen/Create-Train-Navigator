package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class RemoveStationTagEntryPacketData {

    private static final String NBT_DATA = "Data";
    
    private static final String NBT_ID = "Id";
    private static final String NBT_STATION = "Station";

    public static class Request extends NetworkPacketData {
        
        private UUID tagId;
        private String station;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID tagId, String station) {
            super(DLStatus.OK);
            this.tagId = tagId;
            this.station = station;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_ID, tagId);
            nbt.putString(NBT_STATION, station);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.tagId = nbt.getUUID(NBT_ID);
            this.station = nbt.getString(NBT_STATION);
        }
    }

    public static class Response extends NetworkPacketData {
        private Optional<StationTag> tag;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<StationTag> tag) {
            super(DLStatus.OK);
            this.tag = tag;
        }

        @Override
        protected void write(CompoundTag nbt) {
            this.tag.ifPresent(x -> nbt.put(NBT_DATA, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            if (nbt.contains(NBT_DATA)) {
                this.tag = Optional.ofNullable(StationTag.fromNbt(nbt.getCompound(NBT_DATA), null));
            } else {
                this.tag = Optional.empty();
            }
        }

        public Optional<StationTag> getTag() {
            return tag;
        }
        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(Optional.ofNullable(GlobalSettings.getInstance().getStationTag(packet.tagId).map(x -> {
            if (!x.getOwner().isAllowed(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return null;
            }
            x.remove(packet.station);
            x.updateLastEdited(context.getPlayer());
            return x;
        })).orElse(null));
    }
    
}
