package de.mrjulsen.crn.data.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import de.mrjulsen.crn.data.train.TrainState;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.util.IListenable;

public class TransferConnection implements AutoCloseable, IListenable<TransferConnection> {

    public static final String EVENT_CONNECTION_ENDANGERED = "connection_endangered";
    public static final String EVENT_CONNECTION_MISSED = "connection_missed";
    private final Map<String, IdentityHashMap<Object, Consumer<TransferConnection>>> listeners = new HashMap<>();

    private final TrainStop arrival;
    private final TrainStop departure;

    private boolean isPossible = true;

    private boolean wasConnectionEndangered = false;
    private boolean wasConnectionMissed = false;

    public TransferConnection(TrainStop arrival, TrainStop departure) {
        this.arrival = arrival;
        this.departure = departure;

        createEvent(EVENT_CONNECTION_ENDANGERED);
        createEvent(EVENT_CONNECTION_MISSED);
    }

    public static ImmutableList<TransferConnection> getConnections(List<RoutePart> parts) {
        List<TransferConnection> connections = new ArrayList<>();
        for (int i = 0; i < parts.size() - 1; i++) {
            connections.add(new TransferConnection(parts.get(i).getLastStop(), parts.get(i + 1).getFirstStop()));
        }
        return ImmutableList.copyOf(connections);
    }

    public TrainStop getArrivalStation() {
        return arrival;
    }

    public TrainStop getDepartureStation() {
        return departure;
    }

    public long getScheduledTransferTime() {
        return departure.getScheduledDepartureTime() - arrival.getScheduledArrivalTime();
    }

    public long getRealTimeTransferTime() {
        return (departure.shouldRenderRealTime() ? departure.getRealTimeDepartureTime() : departure.getScheduledDepartureTime()) - (arrival.shouldRenderRealTime() ? arrival.getRealTimeArrivalTime() : arrival.getScheduledArrivalTime());
    }

    public boolean isPossible() {
        return isPossible;
    }

    public boolean isConnectionEndangered() {
        return !isPossible() || getRealTimeTransferTime() <= 0;
    }

    public boolean isConnectionMissed() {
        if (!isPossible) {
            return true;
        }

        boolean possible = !(arrival.getState() == TrainState.BEFORE && departure.getState() == TrainState.AFTER);
        if (!possible) {
            isPossible = false;
        }
        return !possible;
    }

    @Override
    public Map<String, IdentityHashMap<Object, Consumer<TransferConnection>>> getListeners() {
        return listeners;
    }

    public void update() {
        if (!isPossible()) {
            return;
        }

        boolean endangered = isConnectionEndangered();
        boolean missed = isConnectionMissed();

        if (endangered != wasConnectionEndangered) {
            notifyListeners(EVENT_CONNECTION_ENDANGERED, this);
        }
        if (missed != wasConnectionMissed) {
            notifyListeners(EVENT_CONNECTION_MISSED, this);
        }

        wasConnectionEndangered = endangered;
        wasConnectionMissed = missed;
    }

    @Override
    public void close() {
        stopListeningAll(this);
    }
}
