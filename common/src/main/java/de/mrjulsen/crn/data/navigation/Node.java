package de.mrjulsen.crn.data.navigation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Objects;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.train.TrainPrediction;

public class Node implements Comparable<Node> {
    private final StationTag tag;
    private final Map<UUID, TrainPrediction> departingTrains = new HashMap<>();

    // Dijkstra
    private long cost = Long.MAX_VALUE;
    private Node previousNode = null;
    //private EdgeData previousEdge = null;
    private final Set<EdgeConnection> connections = new HashSet<>();
    private final Map<Node, EdgeConnection> preferredConnectionForNode = new HashMap<>();

    private boolean isTransferPoint = false;



    public Node(TrainPrediction prediction) {
        this.tag = prediction.getStationTag();
    }

    public void addTrain(TrainPrediction prediction) {
        this.departingTrains.put(prediction.getData().getTrainId(), prediction);
    }

    public Map<UUID, TrainPrediction> getIdsOfDepartingTrains() {
        return departingTrains;
    }

    public StationTag getStationTag() {
        return tag;
    }

    public long getCost() {
        return cost;
    }

    public boolean setCost(long cost) {
        boolean b = this.cost != cost;
        if (b) {
            connections.clear();
            preferredConnectionForNode.clear();
        }
        this.cost = cost;
        return b;
    }

    public Node getPreviousNode() {
        return previousNode;
    }

    public void setConnection(Node previousNode, EdgeData viaEdge, long cost) {
        if (cost > this.cost) {
            return;
        }
        setCost(cost);
        if (viaEdge != null) {
            connections.add(new EdgeConnection(previousNode, viaEdge)); 
        }
        setPreviousNode(previousNode);
    }

    public void setPreviousNode(Node previousNode) {
        this.previousNode = previousNode;
    }

    public boolean isTransferPoint() {
        return isTransferPoint;
    }

    public void setTransferPoint(boolean isTransferPoint) {
        this.isTransferPoint = isTransferPoint;
    }

    public boolean hasConnections() {
        return !this.connections.isEmpty();
    }

    @Override
    public int compareTo(Node o) {
        return Long.compare(cost, o.cost);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Node o &&
            tag.equals(o.tag)
        ;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }

    public void init() {
        this.cost = Long.MAX_VALUE;
        this.previousNode = null;
        this.isTransferPoint = false;
        this.connections.clear();
        this.preferredConnectionForNode.clear();
    }

    public EdgeConnection selectBestConnectionFor(Node nextNode, EdgeData nextEdge) { // Sucht die beste Edge zu diesem Knoten unter Beachtung des nächsten Knotens
        if (!hasConnections()) { // Keine Alternativen
            return null;
        }

        EdgeConnection possibleConnection = getPreviousNode() == null ? null : (getPreviousNode().getPreferredConnectionFor(this));
        
        for (EdgeConnection connection : connections) {
            if (connection.edge() == null) continue;

            if (connection.edge().connected(nextEdge)) { // Wenn es zuvor eine Edge gab, die mit Zug X erreichbar ist, und Zug X auch auf der neuen Edge benötigt wird, wähle diese.
                possibleConnection = connection;
                break;
            }
        }

        if (possibleConnection == null) {
            return null;
        }

        preferredConnectionForNode.put(nextNode, possibleConnection); // Speichere diese Präferenz. Wenn man mit der späteren Node dann hier sucht, bekommt man die Conenction zurück.
        return possibleConnection;
    }

    public EdgeConnection getPreferredConnectionFor(Node node) {
        return preferredConnectionForNode.getOrDefault(node, connections.stream().findFirst().orElse(null));
    }



    public Set<EdgeConnection> getConnections() {
        return connections;
    }

    public EdgeConnection getDeepestConnection() {
        return connections.stream().peek(x -> {
            x.depth = startDepthTest(x.edge());
        }).max(Comparator.comparingInt(EdgeConnection::getDepth)).orElse(null);
    }

    public int startDepthTest(EdgeData ref) {
        return depthTest(this, ref);
    }

    // How long this train can be kept
    public static int depthTest(Node node, EdgeData ref) {
        if (node.getPreviousNode() == null || !node.hasConnections()) {
            return 0;
        }
        if (node.getPreviousNode().connections.stream().noneMatch(x -> x.edge().connected(ref))) {
            return 0;
        }
        return depthTest(node.getPreviousNode(), ref) + 1;
    }


    public static class EdgeConnection {

        private final Node target;
        private final EdgeData edge;
        private int depth = 0;

        public EdgeConnection(Node target, EdgeData edge) {
            this.target = target;
            this.edge = edge;
        }

        public Node target() {
            return target;
        }

        public EdgeData edge() {
            return edge;
        }

        public int getDepth() {
            return depth;
        }
    }

}
