package de.mrjulsen.crn.data.navigation;

import java.util.UUID;

import de.mrjulsen.crn.data.train.TrainPrediction;

public class EdgeData implements Comparable<EdgeData> {
    
    private final TrainPrediction prediction;
    private final Node node1;
    private final Node node2;

    // Calc
    private long cost = -1;

    public EdgeData(Node node1, Node node2, TrainPrediction prediction) {
        this.prediction = prediction;
        this.node1 = node1;
        this.node2 = node2;

        this.cost = prediction.getLastTransitTime();
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
        return getTrainId().equals(other.getTrainId()) && getSectionIndex() == other.getSectionIndex();
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

    @Override
    public int compareTo(EdgeData o) {
        return Long.compare(cost, o.cost);
    }
}
