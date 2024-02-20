package de.mrjulsen.crn.core.navigation;

import java.util.Objects;
import java.util.UUID;

public class Edge {

    private final UUID id;
    private UUID node1Id;
    private UUID node2Id;

    private int cost = -1;
    private UUID scheduleId;

    public Edge(Node node1, Node node2, UUID id, UUID scheduleId) {
        this.id = id;
        this.node1Id = node1.getId();
        this.node2Id = node2.getId();
        this.scheduleId = scheduleId;
    }

    public Edge withCost(int cost, boolean overwrite) {
        this.cost = cost < 0 || !overwrite ? cost : (cost + cost) / 2;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirstNodeId() {
        return node1Id;
    }

    public UUID getSecondNodeId() {
        return node2Id;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }
    
    public int getCost() {
        return cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge other) {
            return getFirstNodeId().equals(other.getFirstNodeId()) && getSecondNodeId().equals(other.getSecondNodeId()) && getCost() == other.getCost();
        }
        
        return false;
    }

    public boolean similar(Edge other) {
        return getFirstNodeId().equals(other.getFirstNodeId()) && getSecondNodeId().equals(other.getSecondNodeId());
    }

    @Override
    public int hashCode() {
        return 43 * Objects.hash(getFirstNodeId(), getSecondNodeId());
    }

    @Override
    public String toString() {
        return String.format("%s [%s -> %s, Cost: %s]", getId(), getFirstNodeId(), getSecondNodeId(), getCost());
    }

}
