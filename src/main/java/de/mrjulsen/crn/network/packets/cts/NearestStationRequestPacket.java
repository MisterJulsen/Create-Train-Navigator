package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.NearestStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class NearestStationRequestPacket implements IPacketBase<NearestStationRequestPacket> {

    public long id;
    public BlockPos pos;

    public NearestStationRequestPacket() { }
    
    public NearestStationRequestPacket(long id, Vec3 pos) {
        this(id, new BlockPos(pos));
    }

    public NearestStationRequestPacket(long id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    @Override
    public void encode(NearestStationRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeBlockPos(packet.pos);
    }

    @Override
    public NearestStationRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        BlockPos pos = buffer.readBlockPos();
        return new NearestStationRequestPacket(id, pos);
    }

    @Override
    public void handle(NearestStationRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            Thread navigationThread = new Thread(() -> {   
                NearestTrackStationResult result = NearestTrackStationResult.empty();
                try {
                    result = TrainUtils.getNearestTrackStation(context.get().getSender().getLevel(), packet.pos);                    
                } catch (Exception e) {
                    ModMain.LOGGER.error("Error while trying to find nearest track station ", e);
                    NetworkManager.getInstance().sendToClient(new ServerErrorPacket(e.getMessage()), context.get().getSender());
                } finally {                    
                    NetworkManager.getInstance().sendToClient(new NearestStationResponsePacket(packet.id, result), context.get().getSender());
                }                
            });
            navigationThread.setPriority(Thread.MIN_PRIORITY);
            navigationThread.setName("Station Location Calculator");
            navigationThread.start();

        });
        
        context.get().setPacketHandled(true);      
    }

    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_SERVER;
    }        
}
