package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Collection;

import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.TrackStationsRequestPacket;

public class ClientTrainStationSnapshot {
    private static ClientTrainStationSnapshot instance;

    private final Collection<String> stationNames;

    private ClientTrainStationSnapshot(Collection<String> stationNames) {
        this.stationNames = stationNames;
    }

    public static ClientTrainStationSnapshot makeNew(Collection<String> stationNames) {
        return instance = new ClientTrainStationSnapshot(stationNames);
    }

    public static ClientTrainStationSnapshot getInstance() {
        if (instance == null) {
            makeNew(new ArrayList<>());
        }
        return instance;
    }

    public Collection<String> getAllTrainStations() {
        return stationNames;
    }

    public static void syncToClient(Runnable then) {
        long id = InstanceManager.registerClientResponseReceievedAction(then);
        NetworkManager.sendToServer(new TrackStationsRequestPacket(id));
    }
}
