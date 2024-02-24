package de.mrjulsen.crn.core.navigation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.data.TrainStationAlias;

public class Node implements Comparable<Node> {
    private TrainStationAlias name;
    private final UUID id;
    private final Set<UUID> trainIds = new HashSet<>();

    // calc
    private long cost = Long.MAX_VALUE;
    private Node previousNode = null;
    private Edge previousEdge = null;
    private boolean isTransferPoint = false;

    public Node(TrainStationAlias alias, UUID id) {
        this.id = id;
        this.name = alias;
    }

    public UUID getId() {
        return id;
    }

    public Set<UUID> getTrainIds() {
        return trainIds;
    }

    public void addTrain(UUID id) {
        trainIds.add(id);
    }

    public TrainStationAlias getStationAlias() {
        return name;
    }

    public void init() {
        this.cost = Long.MAX_VALUE;
        this.previousNode = null;
        this.previousEdge = null;
        this.isTransferPoint = false;
    }

    public long getCost() {
        return cost;
    } 
    
    public void setCost(long cost) {
        this.cost = cost;
    }

    public Node getPreviousNode() {
        return previousNode;
    }

    public Edge getPreviousEdge() {
        return previousEdge;
    }

    public boolean isTransferPoint() {
        return isTransferPoint;
    }

    public void setPrevious(Node node, Edge viaEdge) {
        this.previousNode = node;
        this.previousEdge = viaEdge;
    }

    public void setIsTransferPoint(boolean b) {
        this.isTransferPoint = b;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node other) {
            return getStationAlias().equals(other.getStationAlias());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %s", getStationAlias(), getId(), isTransferPoint() ? "(Transfer)" : "");
    }

    @Override
    public int hashCode() {
        return 37 + Objects.hash(getStationAlias());
    }

    @Override
    public int compareTo(Node o) {
        return Long.compare(getCost(), o.getCost());
    }
}
