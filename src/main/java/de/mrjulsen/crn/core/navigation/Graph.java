package de.mrjulsen.crn.core.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.GlobalTrainData;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.util.TrainUtils;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class Graph {
    public Map<UUID, Node> nodesById;
    public Map<TrainStationAlias, Node> nodesByStation;

    public Map<UUID, Edge> edgesById;
    public Map<Node, Map<Node, Edge>> edgesByNode;

    public Map<UUID, TrainSchedule> schedulesById;
    public Map<UUID, TrainSchedule> schedulesByTrainId;
    public Map<TrainSchedule, Set<UUID>> trainIdsBySchedule;

    public Graph(long updateTime) {
        long startTime = System.currentTimeMillis();

        GlobalTrainData.makeSnapshot(updateTime);

        this.nodesById = new HashMap<>();
        this.nodesByStation = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.edgesByNode = new HashMap<>();
        this.schedulesById = new HashMap<>();
        this.schedulesByTrainId = new HashMap<>();
        this.trainIdsBySchedule = new HashMap<>();

        final int[] trainCounter = new int[] { 0 };
        TrainUtils.getAllTrains().stream().filter(x -> TrainUtils.isTrainValid(x)).forEach(x -> {
            addTrain(x);
            trainCounter[0]++;
        });
        
        long estimatedTime = System.currentTimeMillis() - startTime;
        ModMain.LOGGER.info(String.format("Graph generated. Took %sms. Contains %s nodes, %s edges and %s schedules. %s train processed.",
            estimatedTime,
            nodesById.size(),
            edgesById.size(),
            schedulesById.size(),
            trainCounter
        ));
    }

    protected Node addNode(TrainStationAlias alias) {
        if (nodesByStation.containsKey(alias)) {
            return nodesByStation.get(alias);
        }

        UUID id = UUID.randomUUID();
        while (nodesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Node node = new Node(alias, id);
        nodesById.put(id, node);
        nodesByStation.put(alias, node);
        return node;
    }

    protected Edge addEdge(Node node1, Node node2) {
        UUID id = UUID.randomUUID();
        while (edgesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Edge edge = new Edge(node1, node2, id);
        if (putConnection(node1, node2, edge)) {
            edgesById.put(id, edge);
        }
        return edge;
    }

    protected TrainSchedule addTrain(Train train) {

        if (schedulesByTrainId.containsKey(train.id)) {
            return schedulesByTrainId.get(train.id);
        }

        UUID id = UUID.randomUUID();
        while (edgesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        TrainSchedule schedule = new TrainSchedule(train, id);
        schedule.addToGraph(this, train);

        if (trainIdsBySchedule.containsKey(schedule)) {
            TrainSchedule sched = schedulesByTrainId.get(trainIdsBySchedule.get(schedule).stream().findFirst().get());
            trainIdsBySchedule.get(schedule).add(train.id);
            schedulesByTrainId.put(train.id, sched);

            sched.getEdges().forEach(x -> {
                Optional<Edge> matchingEdge = schedule.getEdges().stream().filter(a -> a.equals(x)).findFirst();

                if (matchingEdge.isPresent()) {
                    x.withCost(matchingEdge.get().getCost(), false);
                }
            });

            return sched;
        }

        schedulesById.put(id, schedule);
        schedulesByTrainId.put(train.id, schedule);
        trainIdsBySchedule.put(schedule, new HashSet<>(Set.of(train.id)));

        return schedule;
    }

    public boolean putConnection(Node node1, Node node2, Edge edge) {
		Map<Node, Edge> connections = edgesByNode.computeIfAbsent(node1, n -> new IdentityHashMap<>());
		if (connections.containsKey(node2)) {
            return false;
        }

		return connections.put(node2, edge) == null;
	}

    public Node getNode(TrainStationAlias alias) {
        return nodesByStation.get(alias);
    }

    public Map<Node, Edge> getConnectionsFrom(Node node) {
		if (node == null) {
			return null;
        }
		return edgesByNode.getOrDefault(node, new HashMap<>());
	}

    public List<TrainStationAlias> navigate(TrainStationAlias start, TrainStationAlias end) {
        Map<TrainStationAlias, Node> nodes = dijkstra(start);
        Node endNode = nodes.get(end);

        List<TrainStationAlias> route = new ArrayList<>();
        route.add(end);

        Node currentNode = endNode;
        while (!currentNode.getStationAlias().equals(start)) {
            route.add(0, currentNode.getPreviousNode().getStationAlias());
            currentNode = currentNode.getPreviousNode();
        }

        return route;
    }

    protected Map<TrainStationAlias, Node> dijkstra(TrainStationAlias start) {
        nodesById.values().forEach(x -> x.init());
        Node startNode = nodesByStation.get(start);
        startNode.setCost(0);
        startNode.setPreviousNode(startNode);

        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<TrainStationAlias, Node> excludedNodes = new IdentityHashMap<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Map<Node, Edge> reachableNodes = edgesByNode.get(currentNode);

            reachableNodes.entrySet().stream().filter(x -> !excludedNodes.containsKey(x.getKey().getStationAlias())).forEach(x -> {
                Node node = x.getKey();
                Edge edge = x.getValue();
                long newCost = currentNode.getCost() + edge.getCost();
                if (newCost > node.getCost()) {
                    return;
                }

                node.setCost(newCost);
                node.setPreviousNode(currentNode);

                queue.add(node);
            });

            excludedNodes.put(currentNode.getStationAlias(), currentNode);
        }

        return excludedNodes;
    }

    public static class GraphData {
        public Collection<Node> nodes;
        public Collection<Edge> edges;

        public GraphData(Graph graph) {
            nodes = graph.nodesById.values();
            edges = graph.edgesById.values();
        }
    }
}
