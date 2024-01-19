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
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.GlobalTrainData;
import de.mrjulsen.crn.data.Route;
import de.mrjulsen.crn.data.RoutePart;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.SimulatedTrainSchedule;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.util.TrainUtils;

public class Graph {
    private Map<UUID, Node> nodesById;
    private Map<TrainStationAlias, Node> nodesByStation;

    private Map<UUID, Edge> edgesById;
    private Map<Node, Map<Node, Set<Edge>>> edgesByNode;

    private Map<UUID, TrainSchedule> schedulesById;
    private Map<UUID, TrainSchedule> schedulesByTrainId;
    private Map<TrainSchedule, Set<UUID>> trainIdsBySchedule;
    private Map<UUID, UUID> scheduleIdByTrainId;

    private final long lastUpdated;

    public Graph(long updateTime) {
        long startTime = System.currentTimeMillis();
        lastUpdated = updateTime;
        GlobalTrainData.makeSnapshot(updateTime);

        this.nodesById = new HashMap<>();
        this.nodesByStation = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.edgesByNode = new HashMap<>();
        this.schedulesById = new HashMap<>();
        this.schedulesByTrainId = new HashMap<>();
        this.trainIdsBySchedule = new HashMap<>();
        this.scheduleIdByTrainId = new HashMap<>();

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
            trainCounter.length
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

    protected Edge addEdge(Node node1, Node node2, UUID scheduleId) {
        UUID id = UUID.randomUUID();
        while (edgesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Edge edge = new Edge(node1, node2, id, scheduleId);
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
            scheduleIdByTrainId.put(train.id, sched.getId());

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
        scheduleIdByTrainId.put(train.id, schedule.getId());

        return schedule;
    }

    public boolean putConnection(Node node1, Node node2, Edge edge) {
		Map<Node, Set<Edge>> connections = edgesByNode.computeIfAbsent(node1, n -> new IdentityHashMap<>());
		if (connections.containsKey(node2)) {
            if (connections.get(node2).contains(edge)) {
                return false;
            }
            return connections.get(node2).add(edge);
        }

		return connections.put(node2, new HashSet<>(Set.of(edge))) == null;
	}

    public Set<Node> getNodes() {
        return new HashSet<>(nodesById.values());
    }

    public Node getNode(TrainStationAlias alias) {
        return nodesByStation.get(alias);
    }

    public Node getNode(UUID id) {
        return nodesById.get(id);
    }

    public Map<Node, Set<Edge>> getEdges(Node node) {
        return edgesByNode.get(node);
    }

    public Set<Edge> getEdges() {
        return new HashSet<>(edgesById.values());
    }

    public Edge getEdge(UUID id) {
        return edgesById.get(id);
    }

    public Map<Node, Set<Edge>> getConnectionsFrom(Node node) {
		if (node == null) {
			return null;
        }
		return edgesByNode.getOrDefault(node, new HashMap<>());
	}

    public Collection<Route> navigate(TrainStationAlias start, TrainStationAlias end, boolean avoidTransfers) {
        return searchTrains(searchRoute(start, end, avoidTransfers));
    }

    public List<Node> searchRoute(TrainStationAlias start, TrainStationAlias end, boolean avoidTransfers) {
        
        if (!nodesByStation.containsKey(start) || !nodesByStation.containsKey(end)) {
            return List.of();
        }

        Map<TrainStationAlias, Node> nodes = dijkstra(start, avoidTransfers);


        Node endNode = nodes.get(end);
        endNode.setIsTransferPoint(true);

        List<Node> route = new ArrayList<>();

        Node currentNode = endNode;
        while (!currentNode.getStationAlias().equals(start)) {
            route.add(0, currentNode);

            if (currentNode.getPreviousEdge() != null) {
                if (currentNode.getPreviousNode().getPreviousEdge() == null) {                
                    currentNode.getPreviousNode().setIsTransferPoint(false);
                } else {
                    currentNode.getPreviousNode().setIsTransferPoint(!currentNode.getPreviousEdge().getScheduleId().equals(currentNode.getPreviousNode().getPreviousEdge().getScheduleId()));
                }
            }
            currentNode = currentNode.getPreviousNode();            
        }
        currentNode.getPreviousNode().setIsTransferPoint(true);
        route.add(0, currentNode.getPreviousNode());

        return route;
    }

    private Map<UUID, SimpleTrainSchedule> generateTrainSchedules() {
        return GlobalTrainData.getInstance().getAllTrains().stream().filter(x -> TrainUtils.isTrainValid(x)).collect(Collectors.toMap(x -> x.id, x -> new SimpleTrainSchedule(x)));
    }

    /*
    public Collection<Route> searchTrains(List<Node> routeNodes) {
        Map<UUID, SimpleTrainSchedule> schedulesByTrain = generateTrainSchedules();
        Set<SimpleTrainSchedule> excludedSchedules = new HashSet<>();
        Route route = new Route(lastUpdated);
        Collection<Route> routes = new ArrayList<>();

        int timer = 0;
        Node lastTransfer = null;
        UUID currentTrainId = null;
        final Node[] filteredTransferNodes = routeNodes.stream().filter(x -> x.isTransferPoint()).toArray(Node[]::new);
        final int len = filteredTransferNodes.length;

        for (int i = 0; i < len; i++) {
            Node node = filteredTransferNodes[i];

            if (lastTransfer != null) {
                final Node lastNode = lastTransfer;
                final int simulationTime = timer;

                Collection<SimulatedTrainSchedule> trainPredictions = GlobalTrainData.getInstance().getDepartingTrainsAt(lastNode.getStationAlias()).stream()
                .filter(x -> {
                    SimpleTrainSchedule schedule = schedulesByTrain.get(x.getTrain().id);

                    boolean b = !x.getTrain().id.equals(currentTrainId) &&
                                !excludedSchedules.contains(schedule) &&
                                schedule.hasStationAlias(node.getStationAlias()) &&
                                TrainUtils.isTrainValid(x.getTrain());                    
                    return b;
                }).map(x -> {
                    return schedulesByTrain.get(x.getTrain().id).simulate(x.getTrain(), 0, lastNode.getStationAlias());
                }).sorted(Comparator.comparingInt(x -> x.getSimulationData().simulationCorrection())).toList();

                SimulatedTrainSchedule selectedPrediction = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), node.getStationAlias())).findFirst().orElse(trainPredictions.stream().findFirst().orElse(null));
                
                if (selectedPrediction == null) {
                    ModMain.LOGGER.warn("Route aborted! No train was found at " + lastNode.getStationAlias().getAliasName());
                    return List.of();
                }

                Collection<SimulatedTrainSchedule> filteredSchedules = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), node.getStationAlias())).toList();
                if (true) {
                    filteredSchedules = List.of(selectedPrediction);
                }

                for (SimulatedTrainSchedule sched : filteredSchedules) {
                    Route r = new Route(lastUpdated);
                    RoutePart part = new RoutePart(sched.getSimulationData().train(), lastNode.getStationAlias(), node.getStationAlias(), simulationTime);
                    route.addPart(part);
                    timer = part.getEndStation().getPrediction().getTicks() + ModClientConfig.TRANSFER_TIME.get();

                    if (i < len) {
                        Collection<RoutePart> parts = searchTrainsIteration(lastTransfer, schedulesByTrain, excludedSchedules, filteredTransferNodes, timer, currentTrainId);
                        parts.forEach(a ->  r.addPart(a));
                    }
                    routes.add(r);
                    excludedSchedules.add(schedulesByTrain.get(part.getTrain().id));
                }
            }

            lastTransfer = node;
        }

        return routes;
    }
    */

    public Collection<Route> searchTrains(List<Node> routeNodes) {
        Map<UUID, SimpleTrainSchedule> schedulesByTrain = generateTrainSchedules();
        Set<SimpleTrainSchedule> excludedSchedules = new HashSet<>();
        Collection<Route> routes = new ArrayList<>();
        routes.add(new Route(lastUpdated));

        int timer = 0;
        UUID currentTrainId = null;
        final Node[] filteredTransferNodes = routeNodes.stream().filter(x -> x.isTransferPoint()).toArray(Node[]::new);
        final int len = filteredTransferNodes.length;

        Node lastTransfer = filteredTransferNodes[0];
        Node node = filteredTransferNodes[1];
        
        final Node lastNode = lastTransfer;
        final int simulationTime = timer;

        Collection<SimulatedTrainSchedule> trainPredictions = GlobalTrainData.getInstance().getDepartingTrainsAt(lastNode.getStationAlias()).stream()
        .filter(x -> {
            SimpleTrainSchedule schedule = schedulesByTrain.get(x.getTrain().id);

            boolean b = !x.getTrain().id.equals(currentTrainId) &&
                        !excludedSchedules.contains(schedule) &&
                        schedule.hasStationAlias(node.getStationAlias()) &&
                        TrainUtils.isTrainValid(x.getTrain());                    
            return b;
        }).map(x -> {
            return schedulesByTrain.get(x.getTrain().id).simulate(x.getTrain(), simulationTime, lastNode.getStationAlias());
        }).sorted(Comparator.comparingInt(x -> x.getSimulationData().simulationCorrection())).toList();

        SimulatedTrainSchedule selectedPrediction = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), node.getStationAlias())).findFirst().orElse(trainPredictions.stream().findFirst().orElse(null));
        
        if (selectedPrediction == null) {
            ModMain.LOGGER.warn("Unable to find route from " + lastNode.getStationAlias().getAliasName());
            return routes;
        }

        Collection<SimulatedTrainSchedule> filteredSchedules = trainPredictions.stream().collect(Collectors.toMap(x -> x.getSimulationData().train().id, x -> x, (o, n) -> {
            if (n.isInDirection(lastNode.getStationAlias(), node.getStationAlias())) {
                return n;
            } else if (o.isInDirection(lastNode.getStationAlias(), node.getStationAlias())) {
                return o;
            }
            return o.getSimulationData().simulationCorrection() < n.getSimulationData().simulationCorrection() ? o : n;
        })).values();
        if (filteredSchedules.isEmpty()) {
            filteredSchedules = List.of(selectedPrediction);
        }

        for (SimulatedTrainSchedule sched : filteredSchedules) {
            Route r = new Route(lastUpdated);
            RoutePart part = new RoutePart(sched.getSimulationData().train(), lastNode.getStationAlias(), node.getStationAlias(), simulationTime);
            r.addPart(part);
            timer = part.getEndStation().getPrediction().getTicks() + ModClientConfig.TRANSFER_TIME.get(); /* TODO Client is not allowed on servers! */
            excludedSchedules.add(schedulesByTrain.get(part.getTrain().id));

            Collection<RoutePart> parts = searchTrainsInternal(schedulesByTrain, new HashSet<>(excludedSchedules), filteredTransferNodes, 2, timer, node, currentTrainId);
            parts.forEach(x -> r.addPart(x));
            routes.add(r);
        }

        return routes;
    }

    public Collection<RoutePart> searchTrainsInternal(Map<UUID, SimpleTrainSchedule> schedulesByTrain, Set<SimpleTrainSchedule> excludedSchedules, Node[] filteredTransferNodes, int startIdx, int timer, Node lastTransfer, UUID currentTrainId) {
        List<RoutePart> routes = new ArrayList<>();

        final int len = filteredTransferNodes.length;
        for (int i = startIdx; i < len; i++) {
            Node node = filteredTransferNodes[i];

            if (lastTransfer != null) {
                final Node lastNode = lastTransfer;
                final int simulationTime = timer;

                Collection<SimulatedTrainSchedule> trainPredictions = GlobalTrainData.getInstance().getDepartingTrainsAt(lastNode.getStationAlias()).stream()
                .filter(x -> {
                    SimpleTrainSchedule schedule = schedulesByTrain.get(x.getTrain().id);

                    boolean b = !x.getTrain().id.equals(currentTrainId) &&
                                !excludedSchedules.contains(schedule) &&
                                schedule.hasStationAlias(node.getStationAlias()) &&
                                TrainUtils.isTrainValid(x.getTrain());                    
                    return b;
                }).map(x -> {
                    return schedulesByTrain.get(x.getTrain().id).simulate(x.getTrain(), simulationTime, lastNode.getStationAlias());
                }).sorted(Comparator.comparingInt(x -> x.getSimulationData().simulationCorrection())).toList();

                SimulatedTrainSchedule selectedPrediction = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), node.getStationAlias())).findFirst().orElse(trainPredictions.stream().findFirst().orElse(null));
                
                if (selectedPrediction == null) {
                    ModMain.LOGGER.warn("Route aborted! No train was found at " + lastNode.getStationAlias().getAliasName());
                    return routes;
                }

                RoutePart part = new RoutePart(selectedPrediction.getSimulationData().train(), lastNode.getStationAlias(), node.getStationAlias(), simulationTime);
                routes.add(part);
                timer = part.getEndStation().getPrediction().getTicks() + ModClientConfig.TRANSFER_TIME.get(); /* TODO Client is not allowed on servers! */
                excludedSchedules.add(schedulesByTrain.get(part.getTrain().id));
            }

            lastTransfer = node;
        }

        return routes;
    }


    protected Map<TrainStationAlias, Node> dijkstra(TrainStationAlias start, boolean avoidTransfers) {
        nodesById.values().forEach(x -> x.init());
        Node startNode = nodesByStation.get(start);
        startNode.setCost(0);
        startNode.setPrevious(startNode, null);
        startNode.setIsTransferPoint(true);

        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<TrainStationAlias, Node> excludedNodes = new HashMap<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Map<Node, Set<Edge>> reachableNodes = edgesByNode.get(currentNode);            

            reachableNodes.entrySet().stream().filter(x -> !excludedNodes.containsKey(x.getKey().getStationAlias())).forEach(y -> {
                final Node node = y.getKey();
                y.getValue().forEach(x -> {
                    Edge edge = x;
                    boolean isTransfer = currentNode.getPreviousEdge() != null && !currentNode.getPreviousEdge().getScheduleId().equals(edge.getScheduleId());

                    TrainSchedule sched = schedulesById.get(edge.getScheduleId());
                    int avgTransferTime = (int)trainIdsBySchedule.get(sched).stream().mapToInt(a -> TrainListener.getInstance().getApproximatedTrainDuration(a)).average().getAsDouble();

                    long newCost = currentNode.getCost() + edge.getCost() + (isTransfer && avoidTransfers ? avgTransferTime + 1000 : 0);
                    if (newCost > node.getCost()) {
                        return;
                    }

                    node.setCost(newCost);
                    node.setPrevious(currentNode, edge);

                    queue.add(node);
                });
            });

            excludedNodes.put(currentNode.getStationAlias(), currentNode);
        }

        return excludedNodes;
    }
}
