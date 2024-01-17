package de.mrjulsen.crn.network;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsUpdatePacket;
import de.mrjulsen.crn.network.packets.cts.NavigationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NearestStationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrackStationsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NavigationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NearestStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NextConnectionsResponsePacket;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.network.packets.stc.TrackStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.TrainDataResponsePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {
    public static final String PROTOCOL_VERSION = String.valueOf(1);
    private static int currentId = 0;

    public static final SimpleChannel MOD_CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(ModMain.MOD_ID, "network_channel")).networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).simpleChannel();
    
    public static void registerNetworkPackets() {
        registerPacket(GlobalSettingsUpdatePacket.class, new GlobalSettingsUpdatePacket());
        registerPacket(GlobalSettingsRequestPacket.class, new GlobalSettingsRequestPacket());
        registerPacket(NavigationRequestPacket.class, new NavigationRequestPacket());
        registerPacket(TrackStationsRequestPacket.class, new TrackStationsRequestPacket());
        registerPacket(NearestStationRequestPacket.class, new NearestStationRequestPacket());
        registerPacket(RealtimeRequestPacket.class, new RealtimeRequestPacket());
        registerPacket(NextConnectionsRequestPacket.class, new NextConnectionsRequestPacket());
        registerPacket(TrainDataRequestPacket.class, new TrainDataRequestPacket());
        
        registerPacket(ServerErrorPacket.class, new ServerErrorPacket());
        registerPacket(GlobalSettingsResponsePacket.class, new GlobalSettingsResponsePacket());
        registerPacket(NavigationResponsePacket.class, new NavigationResponsePacket());
        registerPacket(TrackStationResponsePacket.class, new TrackStationResponsePacket());
        registerPacket(NearestStationResponsePacket.class, new NearestStationResponsePacket());
        registerPacket(RealtimeResponsePacket.class, new RealtimeResponsePacket());
        registerPacket(NextConnectionsResponsePacket.class, new NextConnectionsResponsePacket());
        registerPacket(TrainDataResponsePacket.class, new TrainDataResponsePacket());
        
    }

    public static SimpleChannel getPlayChannel() {
        return MOD_CHANNEL;
    }

    private static <T> void registerPacket(Class<T> clazz, IPacketBase<T> packet) {
        MOD_CHANNEL.registerMessage(currentId++, clazz, packet::encode, packet::decode, packet::handle);
    }

    public static <T> void sendToClient(IPacketBase<T> o, ServerPlayer player) {
        NetworkManager.MOD_CHANNEL.sendTo(o, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <T> void sendToServer(IPacketBase<T> o) {
        NetworkManager.MOD_CHANNEL.sendToServer(o);
    }

    public static void executeOnClient(Runnable task) {        
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> task.run());
    }

    
}


