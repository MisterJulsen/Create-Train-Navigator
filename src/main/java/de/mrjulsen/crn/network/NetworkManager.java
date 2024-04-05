package de.mrjulsen.crn.network;

import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
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
import de.mrjulsen.crn.network.packets.stc.TimeCorrectionPacket;
import de.mrjulsen.crn.network.packets.stc.TrackStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.TrainDataResponsePacket;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.mcdragonlib.network.NetworkManagerBase;

public class NetworkManager extends NetworkManagerBase<NetworkManager> {

    private static NetworkManager instance;

    public NetworkManager(String modid, String channelName, String protocolVersion) {
        super(modid, channelName, protocolVersion);
    }

    public static void create() {
        instance = NetworkManagerBase.create(NetworkManager.class, ModMain.MOD_ID, "crn_network_channel", "5");
    }

    public static NetworkManager getInstance() {
        return instance;
    }

    @Override
    public Collection<Class<? extends IPacketBase<?>>> packets() {
        return List.of(
            // cts
            GlobalSettingsRequestPacket.class,
            GlobalSettingsUpdatePacket.class,
            NavigationRequestPacket.class,
            NearestStationRequestPacket.class,
            NextConnectionsRequestPacket.class,
            RealtimeRequestPacket.class,
            TrackStationsRequestPacket.class,
            TrainDataRequestPacket.class,
            AdvancedDisplayUpdatePacket.class,

            // stc
            GlobalSettingsResponsePacket.class,
            NavigationResponsePacket.class,
            NearestStationResponsePacket.class,
            NextConnectionsResponsePacket.class,
            RealtimeResponsePacket.class,
            ServerErrorPacket.class,
            TrackStationResponsePacket.class,
            TrainDataResponsePacket.class,
            TimeCorrectionPacket.class
        );
    }
}


