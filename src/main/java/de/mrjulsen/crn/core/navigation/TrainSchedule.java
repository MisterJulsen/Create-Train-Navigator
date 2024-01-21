package de.mrjulsen.crn.core.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalTrainData;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.event.listeners.TrainListener;

public class TrainSchedule {
    
    private final UUID id;
    private Set<Node> nodes;
    private Set<Edge> edges;

    private List<TrainStop> stops;
    
    public TrainSchedule(Train train, UUID id, GlobalSettings settingsInstance) {
        this.id = id;
        nodes = ConcurrentHashMap.newKeySet();
        edges = ConcurrentHashMap.newKeySet();
        makeSchedule(train, settingsInstance);        
    }

    private void makeSchedule(Train train, GlobalSettings settingsInstance) {
        this.stops = new ArrayList<>(GlobalTrainData.getInstance().getAllStopsSorted(train).stream().filter(x -> !settingsInstance.isBlacklisted(x.getStationAlias())).toList());
        System.out.println("Stop size: " + train.name.getString() + " " + stops.size());
    }

    public boolean addToGraph(Graph graph, Train train) {
        if (stops.isEmpty()) {
            return false;
        }

        final int cycleDuration = TrainListener.getInstance().getApproximatedTrainDuration(train);

        final int size = stops.size();
        TrainStop lastStop = stops.get(size - 1);
        
        for (int i = 0; i < size; i++) {
            TrainStop stop = stops.get(i);

            int duration = i == 0 ? cycleDuration - lastStop.getPrediction().getTicks() + stop.getPrediction().getTicks() : stop.getPrediction().getTicks() - lastStop.getPrediction().getTicks();
            Node node1 = graph.addNode(lastStop.getStationAlias());
            Node node2 = graph.addNode(stop.getStationAlias());
            Edge edge = graph.addEdge(node1, node2, getId()).withCost(duration, false);

            nodes.add(node1);
            nodes.add(node2);
            edges.add(edge);

            lastStop = stop;
        }

        return true;
    }

    public UUID getId() {
        return id;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Set<Edge> getEdges() {
        return edges;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrainSchedule other) {
            boolean b = getNodes().size() == other.getNodes().size() && getEdges().size() == other.getEdges().size();
            return b && getNodes().containsAll(other.getNodes()) && getEdges().containsAll(other.getEdges());
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        return 41 * Objects.hash(getEdges(), getNodes());
    }

    public void debugPrint() {
        System.out.println(String.format("TRAIN SCHEDULE DETAILS (%s nodes, %s edges)", nodes.size(), edges.size()));
        System.out.println("Nodes");
        for (Node node : nodes) {
            System.out.println(" -> " + node);
        }

        System.out.println("Edges");
        for (Edge edge : edges) {
            System.out.println(" -> " + edge);
        }

        System.out.println("--- END OF SCHEDULE ---");
    }
}
