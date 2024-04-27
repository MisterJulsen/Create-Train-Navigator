package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Collection;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.packets.cts.TrackStationsRequestPacket;

public class ClientTrainStationSnapshot {
    private static ClientTrainStationSnapshot instance;

    private final Collection<String> stationNames;
    private final Collection<String> trainNames;

    private final int listeningTrainCount;
    private final int totalTrainCount;

    private ClientTrainStationSnapshot(Collection<String> stationNames, Collection<String> trainNames, int listeningTrainCount, int totalTrainCount) {
        this.stationNames = stationNames;
        this.trainNames = trainNames;
        this.listeningTrainCount = listeningTrainCount;
        this.totalTrainCount = totalTrainCount;
    }

    public static ClientTrainStationSnapshot makeNew(Collection<String> stationNames, Collection<String> trainNames, int listeningTrainCount, int totalTrainCount) {
        return instance = new ClientTrainStationSnapshot(stationNames, trainNames, listeningTrainCount, totalTrainCount);
    }

    public static ClientTrainStationSnapshot getInstance() {
        if (instance == null) {
            makeNew(new ArrayList<>(), new ArrayList<>(), 0, 0);
        }
        return instance;
    }

    public Collection<String> getAllTrainStations() {
        return stationNames;
    }

    public Collection<String> getAllTrainNames() {
        return trainNames;
    }

    public static void syncToClient(Runnable then) {
        long id = InstanceManager.registerClientResponseReceievedAction(then);
        ExampleMod.net().CHANNEL.sendToServer(new TrackStationsRequestPacket(id));
    }


    public int getListeningTrainCount() {
        return listeningTrainCount;
    }

    public int getTrainCount() {
        return totalTrainCount;
    }

    public int getStationCount() {
        return stationNames.size();
    }

    public void dispose() {
        instance = null;
    }
}
