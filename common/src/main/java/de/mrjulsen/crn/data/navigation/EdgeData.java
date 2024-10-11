package de.mrjulsen.crn.data.navigation;

import java.util.UUID;

import de.mrjulsen.crn.data.train.TrainPrediction;

public class EdgeData implements Comparable<EdgeData> {
    
    private final TrainPrediction prediction;
    private final Node node1;
    private final Node node2;

    // Calc
    private long cost = -1;

    protected EdgeData(Node node1, Node node2, TrainPrediction prediction, long cost) {
        this.prediction = prediction;
        this.node1 = node1;
        this.node2 = node2;
        this.cost = cost;
    }

    public EdgeData(Node node1, Node node2, TrainPrediction prediction) {
        this(node1, node2, prediction, prediction.getLastTransitTime());
    }

    public Node getFirstNode() {
        return node1;
    }

    public Node getSecondNode() {
        return node2;
    }

    public UUID getTrainId() {
        return prediction.getData().getTrainId();
    }

    public boolean connected(EdgeData other) {
        return getTrainId().equals(other.getTrainId()) && (getSectionIndex() == other.getSectionIndex());
    }

    public int getSectionIndex() {
        return prediction.getSection().getScheduleIndex();
    }
    
    public TrainPrediction getPrediction() {
        return prediction;
    }

    public long getCost() {
        return cost;
    }

    public EdgeData invert() {
        return new EdgeData(node2, node1, prediction);
    }

    @Override
    public int compareTo(EdgeData o) {
        return Long.compare(cost, o.cost);
    }
}
