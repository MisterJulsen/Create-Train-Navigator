package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class CreateStationTagPacketData {

    private static final String NBT_DATA = "Data";
    private static final String NBT_NAME = "Name";
    private static final String NBT_OWNER = "Owner";

    public static class Request extends NetworkPacketData {
        private String name;
        private Optional<Owner> owner;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(String name, Optional<Owner> owner) {
            super(DLStatus.OK);
            this.name = name;
            this.owner = owner;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString(NBT_NAME, name);
            this.owner.ifPresent(x -> nbt.put(NBT_OWNER, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = nbt.getString(NBT_NAME);
            this.owner = Optional.ofNullable(nbt.contains(NBT_OWNER) ? Owner.fromNbt(nbt.getCompound(NBT_OWNER)) : null);
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
        if (!GlobalSettings.modificationsAllowed(context.getPlayer())) {
            return new Response(Optional.empty());
        }
        return new Response(Optional.ofNullable(GlobalSettings.getInstance().createOrGetStationTag(TagName.of(packet.name), packet.owner.orElse(null))));
    }
    
}
