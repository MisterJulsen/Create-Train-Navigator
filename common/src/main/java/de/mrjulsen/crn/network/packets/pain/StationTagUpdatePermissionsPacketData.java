package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Lock.LockState;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class StationTagUpdatePermissionsPacketData {

    private static final String NBT_DATA = "Data";
    
    private static final String NBT_ID = "Id";
    private static final String NBT_STATE = "State";
    private static final String NBT_NEW_OWNER = "NewOwner";
    private static final String NBT_TRUSTED = "Trusted";

    public static class Request extends NetworkPacketData {
        
        private UUID id;
        private Owner newOwner;
        private LockState state;
        private Set<Owner> trusted;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID id, Owner newOwner, LockState state, Set<Owner> trusted) {
            super(DLStatus.OK);
            this.id = id;
            this.newOwner = newOwner;
            this.state = state;
            this.trusted = trusted;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_ID, id);
            if (state != null) {
                nbt.putByte(NBT_STATE, state.getIndex());
            }
            if (trusted != null) {
                ListTag list = new ListTag();
                for (Owner t : trusted) {
                    list.add(t.toNbt());
                }
                nbt.put(NBT_TRUSTED, list);
            }
            if (newOwner != null) nbt.put(NBT_NEW_OWNER, newOwner.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.id = nbt.getUUID(NBT_ID);
            this.newOwner = nbt.contains(NBT_NEW_OWNER) ? Owner.fromNbt(nbt.getCompound(NBT_NEW_OWNER)) : null;
            this.state = nbt.contains(NBT_STATE) ? LockState.getByIndex(nbt.getByte(NBT_STATE)) : null;
            this.trusted = nbt.contains(NBT_TRUSTED) ? nbt.getList(NBT_TRUSTED, Tag.TAG_COMPOUND).stream().map(x -> Owner.fromNbt((CompoundTag)x)).collect(Collectors.toSet()) : null;
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
        return new Response(Optional.ofNullable(GlobalSettings.getInstance().getStationTag(packet.id).map((tag) -> {
            if (!tag.getOwner().isAdmin(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return null;
            }
            if (packet.state != null) tag.getOwner().set(packet.state);
            if (packet.trusted != null) tag.getOwner().updateTrusted(packet.trusted);
            if (packet.newOwner != null) {
                tag.getOwner().setOwner(packet.newOwner);
                tag.getOwner().addTrusted(new Owner(context.getPlayer()));
            }
            return tag;
        })).orElse(null));
    }
    
}
