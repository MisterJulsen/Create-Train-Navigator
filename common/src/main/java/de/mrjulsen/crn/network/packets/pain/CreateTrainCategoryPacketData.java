package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class CreateTrainCategoryPacketData {

    private static final String NBT_DATA = "Data";
    
    private static final String NBT_NAME = "Name";

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
            nbt.putString(NBT_NAME, name);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = nbt.getString(NBT_NAME);
        }
    }

    public static class Response extends NetworkPacketData {
        private Optional<TrainCategory> category;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<TrainCategory> category) {
            super(DLStatus.OK);
            this.category = category;
        }

        @Override
        protected void write(CompoundTag nbt) {
            this.category.ifPresent(x -> nbt.put(NBT_DATA, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            if (nbt.contains(NBT_DATA)) {
                this.category = Optional.ofNullable(TrainCategory.fromNbt(nbt.getCompound(NBT_DATA)));
            } else {
                this.category = Optional.empty();
            }
        }

        public Optional<TrainCategory> getCategory() {
            return category;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        if (!GlobalSettings.modificationsAllowed(context.getPlayer())) {
            return new Response(Optional.empty());
        }
        TrainCategory category = GlobalSettings.getInstance().createOrGetTrainCategory(packet.name, new Owner(context.getPlayer()));
        return new Response(Optional.ofNullable(category));
    }
    
}
