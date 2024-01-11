package de.mrjulsen.crn.core.navigation;

import java.util.Objects;
import java.util.UUID;

import de.mrjulsen.crn.data.TrainStationAlias;

public class Node implements Comparable<Node> {
    private TrainStationAlias name;
    private final UUID id;

    // calc
    private long cost = Long.MAX_VALUE;
    private Node previous = null;

    public Node(TrainStationAlias alias, UUID id) {
        this.id = id;
        this.name = alias;
    }

    public UUID getId() {
        return id;
    }

    public TrainStationAlias getStationAlias() {
        return name;
    }

    public void init() {
        this.cost = Long.MAX_VALUE;
        this.previous = null;
    }

    public long getCost() {
        return cost;
    } 
    
    public void setCost(long cost) {
        this.cost = cost;
    }

    public Node getPreviousNode() {
        return previous;
    }

    public void setPreviousNode(Node node) {
        this.previous = node;
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
        return String.format("%s (%s)", getStationAlias(), getId());
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
