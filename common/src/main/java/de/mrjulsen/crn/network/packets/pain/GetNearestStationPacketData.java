package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class GetNearestStationPacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {

        private BlockPos pos;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(BlockPos pos) {
            super(DLStatus.OK);
            this.pos = pos;
        }

        @Override
        protected void write(CompoundTag nbt) {
            NbtUtils.putNbtPos(nbt, NBT_DATA, pos);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.pos = NbtUtils.getNbtBlockPos(nbt, NBT_DATA);
        }
    }

    public static class Response extends NetworkPacketData {
        private NearestTrackStationResult result;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(NearestTrackStationResult result) {
            super(DLStatus.OK);
            this.result = result;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, result.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.result = NearestTrackStationResult.fromNbt(nbt.getCompound(NBT_DATA));
        }

        public NearestTrackStationResult getResult() {
            return result;
        }        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        NearestTrackStationResult result = NearestTrackStationResult.empty();
        try {
            result = TrainUtils.getNearestTrackStation(context.getPlayer().level(), packet.pos);                    
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Error while trying to find nearest track station.", e);
            ModNetworkManager.SERVER_ERROR.send(NetworkDirection.toPlayer((ServerPlayer)context.getPlayer()), new ServerErrorPacketData(e.getMessage()));
        }
        return new Response(result);
    }
    
}
