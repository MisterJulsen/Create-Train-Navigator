package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetUserSettingsPacketData {

    private static final String NBT_DATA = "Data";
    private static final String NBT_ID = "Id";

    public static class Request extends NetworkPacketData {
        
        private UUID id;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID id) {
            super(DLStatus.OK);
            this.id = id;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_ID, id);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.id = nbt.getUUID(NBT_ID);
        }
    }

    public static class Response extends NetworkPacketData {
        private Optional<UserSettings> data;
        private UUID playerId;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<UserSettings> data) {
            super(DLStatus.OK);
            this.data = data;
            this.playerId = data.map(x -> x.getOwnerId()).orElse(new UUID(0, 0));
        }

        @Override
        protected void write(CompoundTag nbt) {
            data.ifPresent(x -> nbt.put(NBT_DATA, x.toNbt()));
            nbt.putUUID(NBT_ID, playerId);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = Optional.ofNullable(nbt.contains(NBT_DATA) ? UserSettings.fromNbt(nbt.getCompound(NBT_DATA), nbt.getUUID(NBT_ID), false) : null);
            this.playerId = nbt.getUUID(NBT_ID);
        }

        public Optional<UserSettings> getData() {
            return data;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(Optional.ofNullable(UserSettings.getSettingsFor(packet.id, false)));
    }
    
}
