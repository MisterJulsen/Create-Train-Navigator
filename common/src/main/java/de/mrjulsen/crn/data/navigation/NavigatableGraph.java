package de.mrjulsen.crn.data.navigation;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.data.navigation.Node.EdgeConnection;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;

/* #######################################################
 * 
 *      § 1: Das Navigationssystem ist unantastbar!
 * 
 * #######################################################
 */ 

public class NavigatableGraph {

    protected final UserSettings userSettings;

    protected final Map<StationTag, Node> nodesByTag = new HashMap<>();
    protected final Map<UUID, Set<Node>> nodesByTrain = new HashMap<>();
    protected final Map<Node /* src */, Map<Node /* target */, Set<EdgeData> /* connection */>> edgesByTag = new HashMap<>();
    
    //#region GRAPH GENERATION

    /** Server-side only! */
    public NavigatableGraph(UserSettings userSettings) throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        this.userSettings = userSettings;
        long startTime = System.currentTimeMillis();
        Set<Train> trains = TrainListener.getAllTrains().stream().filter(x ->
            !globalSettings().isTrainBlacklisted(x) &&
            !globalSettings().isTrainExcludedByUser(x, userSettings) &&
            TrainUtils.isTrainUsable(x)
        ).collect(Collectors.toSet());
        for (Train train : trains) {
            addTrain(train, TrainListener.data.get(train.id));
        }

        CreateRailwaysNavigator.LOGGER.info(String.format("Graph generated. Took %sms. Contains %s nodes and %s edges. %s train processed.",
            System.currentTimeMillis() - startTime,
            nodesByTag.size(),
            edgesByTag.values().stream().flatMap(x -> x.values().stream().flatMap(y -> y.stream())).count(),
            trains.size()
        ));
    }

    protected GlobalSettings globalSettings() {
        return GlobalSettings.getInstance();
    }

    protected void addTrain(Train train, TrainData data) {
        Deque<TrainPrediction> predictions = new ConcurrentLinkedDeque<>(data.getPredictions());
        boolean singleSection = data.isSingleSection();
        if (predictions.isEmpty()) {
            return;
        }

        if (CreateRailwaysNavigator.isDebug()) CreateRailwaysNavigator.LOGGER.info("\nEDGES FOR TRAIN: " + train.name.getString() + ", Single Section: " + singleSection + ", Sections Count: " + data.getSections().size());
        StringBuilder sb = new StringBuilder();

        TrainPrediction lastPrediction = null; // The last prediction in the list
        boolean nothingFound = true;
        boolean stationsRemoved = false;
        while (!predictions.isEmpty()) {
            TrainPrediction prediction = predictions.peekLast();
            TrainTravelSection section = prediction.getSection();
            if ((globalSettings().isStationBlacklisted(prediction.getStationName())) ||
                (!section.isUsable() && (!section.isFirstStop(prediction) || !section.previousSection().isUsable() || !section.previousSection().shouldIncludeNextStationOfNextSection()))
            ) {
                predictions.removeLast();
                stationsRemoved = true;
                continue;
            }
            
            nothingFound = false;
            lastPrediction = prediction;
            break;
        }
        if (nothingFound || lastPrediction == null) {
            return;
        }

        Node lastNode = addNode(lastPrediction);
        TrainPrediction lastTrainPrediction = lastPrediction;
        if (CreateRailwaysNavigator.isDebug()) sb.append(lastTrainPrediction.getStationTag().getTagName().get());
        boolean noEdgePossible = false;
        while (!predictions.isEmpty()) {
            TrainPrediction prediction = predictions.poll();
            if (!isPredictionAllowed(prediction)) {
                noEdgePossible = true;
                continue;
            }
            Node node = addNode(prediction);
            if ((!noEdgePossible && !stationsRemoved && lastTrainPrediction.getSection().getScheduleIndex() == prediction.getSection().getScheduleIndex()) || lastTrainPrediction.getSection().shouldIncludeNextStationOfNextSection()) {
                addEdge(lastNode, node, prediction);
                if (CreateRailwaysNavigator.isDebug()) sb.append(" ----- ");
            } else {
                if (CreateRailwaysNavigator.isDebug()) sb.append(" >   < ");
            }
            lastNode = node;
            lastTrainPrediction = prediction;
            noEdgePossible = false;
            stationsRemoved = false;
            if (CreateRailwaysNavigator.isDebug()) sb.append(prediction.getStationTag().getTagName().get());
        }
        if (CreateRailwaysNavigator.isDebug()) CreateRailwaysNavigator.LOGGER.info(sb.toString());
    }

    protected boolean isPredictionAllowed(TrainPrediction prediction) {
        TrainTravelSection section = prediction.getSection();
        boolean usable = section.isUsable() || (section.isFirstStop(prediction) && section.previousSection().isUsable() && section.previousSection().shouldIncludeNextStationOfNextSection());
        return !globalSettings().isStationBlacklisted(prediction.getStationName()) && (prediction.getSection().getTrainGroup() == null || !userSettings.navigationExcludedTrainGroups.getValue().contains(prediction.getSection().getTrainGroup().getGroupName())) && usable;
    }

    protected Node addNode(TrainPrediction prediction) {
        StationTag tag = prediction.getStationTag();
        Node node = nodesByTag.computeIfAbsent(tag, x -> new Node(prediction));
        nodesByTrain.computeIfAbsent(prediction.getData().getTrainId(), x -> new HashSet<>()).add(node);
        node.addTrain(prediction);
        return node;
    }

    protected void addEdge(Node first, Node second, TrainPrediction prediction) {
        if (first == second) {
            return;
        }
        edgesByTag.computeIfAbsent(first, x -> new HashMap<>()).computeIfAbsent(second, x -> new HashSet<>()).add(new EdgeData(first, second, prediction));
    }

//#endregion
//#region GRAPH ACCESSORS

    protected Map<StationTag, Node> dijkstra(StationTag start, boolean avoidTransfers) {
        nodesByTag.values().forEach(x -> x.init());
        Node startNode = nodesByTag.get(start);
        startNode.setConnection(startNode, null, 0);
        startNode.setTransferPoint(true);

        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<StationTag, Node> excludedNodes = new HashMap<>();
        queue.add(startNode);
        
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Map<Node /* target */, Set<EdgeData> /* via */> reachableNodes = edgesByTag.get(currentNode);

            if (reachableNodes != null) {                
                reachableNodes.entrySet().stream().filter(x -> !excludedNodes.containsKey(x.getKey().getStationTag())).forEach(y -> {
                    final Node targetNode = y.getKey();
                    y.getValue().forEach(x -> {
                        final EdgeData viaEdge = x;

                        EdgeConnection connection = currentNode.selectBestConnectionFor(targetNode, viaEdge);
                        boolean isTransfer = connection != null && !connection.edge().connected(viaEdge);
                        
                        // TODO: Evtl die kürzeste Umstiegszeit (also Kosten für den Umstieg) berechnen lassen anhand der aktuellen Verkehrslage

                        long newCost = currentNode.getCost() + viaEdge.getCost() + (isTransfer && avoidTransfers ? ModCommonConfig.TRANSFER_COST.get() : 0);
                        targetNode.setConnection(currentNode, viaEdge, newCost);
                    });
                    queue.add(targetNode);
                });
            }

            excludedNodes.put(currentNode.getStationTag(), currentNode);
        }

        return excludedNodes;
    }

    protected Node dijkstraProcessor(Map<StationTag, Node> nodes, StationTag start, StationTag end) {
        if (nodes.size() <= 1 || !nodes.containsKey(end) || !nodes.containsKey(start)) {
            return null;
        }
        
        Node currentNode = nodes.get(end);
        while (!currentNode.getStationTag().equals(start)) {
            Node prevNode = currentNode.getPreviousNode();
            if (prevNode == null || currentNode == prevNode) break;
            prevNode.setNextNode(currentNode);
            currentNode = prevNode;
        }
        return currentNode;
    }

    public List<Node> searchRoute(StationTag start, StationTag end, boolean avoidTransfers) {        
        if (!nodesByTag.containsKey(start) || !nodesByTag.containsKey(end)) {
            return List.of();
        }

        Node startNode = dijkstraProcessor(dijkstra(start, avoidTransfers), start, end);
        
        if (CreateRailwaysNavigator.isDebug()) {            
            StringBuilder sb = new StringBuilder("Dijkstra Nodes: ");
            Node node = startNode;
            while (node != null) {
                sb.append(node.getStationTag().getTagName().get() + " > ");
                node = node.getNextNode();
            }
            CreateRailwaysNavigator.LOGGER.info(sb.toString());
        }

        if (startNode == null) {
            return List.of();
        }
        startNode.setTransferPoint(true);

        List<Node> route = new ArrayList<>();
        Node currentNode = startNode;
        while (!currentNode.getStationTag().equals(end)) {
            route.add(currentNode);
            MutableSingle<Node> nextNode = new MutableSingle<Node>(currentNode.getNextNode());
            List<EdgeConnection> connections = new ArrayList<>(currentNode.getNextConnections().stream().filter(x -> x.edge().getSecondNode().equals(nextNode.getFirst())).toList());
            while (nextNode.getFirst() != null && connections != null && !connections.isEmpty()) {
                Node pNode = nextNode.getFirst().getNextNode();
                if (pNode == null) {
                    break;
                }
                List<EdgeConnection> pConnections = nextNode.getFirst().getNextConnections().stream().filter(x -> x.edge().getSecondNode().equals(pNode)).toList();
                connections.removeIf(x -> pConnections.stream().noneMatch(y -> x.edge().connected(y.edge())));
                if (connections.isEmpty()) break;
                nextNode.setFirst(pNode);
            }

            currentNode = nextNode.getFirst();
            currentNode.setTransferPoint(true);
        }
        currentNode.setTransferPoint(true);
        route.add(currentNode);

        return route;
    }

    public ImmutableMap<UUID, TrainSchedule> createTrainSchedules() {
        return ImmutableMap.copyOf(TrainUtils.getTrains(true).stream()
            .filter(x -> {
                return  TrainListener.data.containsKey(x.id) &&
                        TrainUtils.isTrainUsable(x) &&
                        !globalSettings().isTrainBlacklisted(x) && 
                        !globalSettings().isTrainExcludedByUser(x, userSettings)
                    ;
            })
            .map(x -> new TrainSchedule(TrainListener.data.get(x.id).getSessionId(), x))
            .collect(Collectors.toMap(x -> x.getTrain().id, x -> x)));
    }

    public List<Route> searchTrainsForRoute(List<Node> nodes) {
        ImmutableMap<UUID, TrainSchedule> schedules = createTrainSchedules();

        StringBuilder sb = new StringBuilder("Transfer points: ");
        Queue<Node> transferNodes = new ConcurrentLinkedQueue<>(nodes.stream().filter(x -> x.isTransferPoint()).peek(x -> {
            if (CreateRailwaysNavigator.isDebug()) sb.append(x.getStationTag().getTagName().get() + " > ");
        }).toList());
        if (CreateRailwaysNavigator.isDebug()) CreateRailwaysNavigator.LOGGER.info(sb.toString());

        if (transferNodes.size() <= 1) {
            return List.of();
        }

        Node start = transferNodes.poll();
        Node end = transferNodes.poll();

        Set<UUID> excludedTrainIds = new HashSet<>();
        List<Train> departingTrains = TrainUtils.getDepartingTrainsAt(start.getStationTag()).stream().filter(train -> 
            TrainListener.data.containsKey(train.id) &&
            TrainUtils.isTrainUsable(train) &&
            !excludedTrainIds.contains(train.id) &&
            !globalSettings().isTrainBlacklisted(train) &&
            schedules.containsKey(train.id) &&
            schedules.get(train.id).stopsAt(start.getStationTag()) &&
            schedules.get(train.id).stopsAt(end.getStationTag())
        ).toList();
        
        List<Route> routes = new ArrayList<>();

        for (Train train : departingTrains) {
            TrainData trainData = TrainListener.data.get(train.id);
            int simulationTime = userSettings.navigationDepartureInTicks.getValue();
            Queue<Node> tempTransferNodes = new ConcurrentLinkedQueue<>(transferNodes);
            Node tempEnd = end;

            RoutePart part = RoutePart.get(schedules.get(train.id).getSessionId(), schedules.get(train.id).simulate(simulationTime), start.getStationTag(), end.getStationTag(), userSettings);  
            if (!RoutePart.validate(part, trainData)) {
                continue;
            }
            
            while (!tempTransferNodes.isEmpty()) {
                RoutePart tempPart = RoutePart.get(schedules.get(train.id).getSessionId(), schedules.get(train.id).simulate(simulationTime), start.getStationTag(), tempTransferNodes.peek().getStationTag(), userSettings);  
                if (!RoutePart.validate(tempPart, trainData)) {
                    break;
                }
                part = tempPart;
                tempEnd = tempTransferNodes.poll();
            }


            if (ModCommonConfig.EXCLUDE_TRAINS.get()) excludedTrainIds.add(train.id);

            // Step 2
            List<RoutePart> parts = new ArrayList<>();
            parts.add(part);
            if (!tempTransferNodes.isEmpty()) {
                Set<UUID> exclTrns = new HashSet<>(excludedTrainIds);
                if (ModCommonConfig.EXCLUDE_TRAINS.get()) exclTrns.add(part.getTrainId());
                List<RoutePart> res = searchForTrainsInternal(tempEnd, schedules, new ConcurrentLinkedQueue<>(tempTransferNodes), exclTrns, part);
                if (res == null) continue;                
                parts.addAll(res);
            }
            routes.add(new Route(parts, false));
        }

        return routes;
    }

    public List<RoutePart> searchForTrainsInternal(Node start, ImmutableMap<UUID, TrainSchedule> schedules, Queue<Node> transferNodes, Set<UUID> excludedTrainIds, RoutePart previousPart) {
        Node end = transferNodes.poll();

        List<Train> departingTrains = TrainUtils.getDepartingTrainsAt(start.getStationTag()).stream().filter(train ->
            TrainListener.data.containsKey(train.id) && 
            TrainUtils.isTrainUsable(train) &&
            !excludedTrainIds.contains(train.id) &&
            !globalSettings().isTrainBlacklisted(train) &&
            schedules.containsKey(train.id) &&
            schedules.get(train.id).stopsAt(start.getStationTag()) &&
            schedules.get(train.id).stopsAt(end.getStationTag())
        ).toList();

        RoutePart bestPart = null;
        Node bestPartEnd = null;
        Queue<Node> bestPartRemainingTransfers = null;

        for (Train train : departingTrains) {
            TrainData trainData = TrainListener.data.get(train.id);
            long simulationTime = previousPart.timeUntilEnd() - 1 + userSettings.navigationTransferTime.getValue();  
            Queue<Node> tempTransferNodes = new ConcurrentLinkedQueue<>(transferNodes);
            Node tempEnd = end;

            RoutePart part = RoutePart.get(schedules.get(train.id).getSessionId(), schedules.get(train.id).simulate(simulationTime), start.getStationTag(), tempEnd.getStationTag(), userSettings); 
            if (!RoutePart.validate(part, trainData)) {
                continue;
            }
            
            while (!tempTransferNodes.isEmpty()) {
                RoutePart tempPart = RoutePart.get(schedules.get(train.id).getSessionId(), schedules.get(train.id).simulate(simulationTime), start.getStationTag(), tempTransferNodes.peek().getStationTag(), userSettings);  
                if (!RoutePart.validate(tempPart, trainData)) {
                    break;
                }
                part = tempPart;
                tempEnd = tempTransferNodes.poll();
            }

            if (bestPart == null || part.compareTo(bestPart) <= 0) {
                bestPart = part;
                bestPartEnd = tempEnd;
                bestPartRemainingTransfers = tempTransferNodes;
            }
        }
        
        if (bestPart == null || bestPart.isEmpty()) {
            return null;
        }

        if (ModCommonConfig.EXCLUDE_TRAINS.get()) excludedTrainIds.add(bestPart.getTrainId());
        List<RoutePart> parts = new ArrayList<>();
        parts.add(bestPart);
        if (bestPartRemainingTransfers != null && !bestPartRemainingTransfers.isEmpty()) {
            List<RoutePart> res = searchForTrainsInternal(bestPartEnd, schedules, bestPartRemainingTransfers, excludedTrainIds, bestPart);
            if (res == null) return null;
            parts.addAll(res);
        }
        return parts;
    }

    /**
     * Generates possible routes from the start station to the destination station.
     * @param start The start station.
     * @param destination The destination station.
     * @param playerId The UUID of the player to use its user settings, or {@code null} to use the default settings.
     * @param avoidTransfers Tries to avoid transfers as much as possible.
     * @return The possible routes.
     */
    public static List<Route> searchRoutes(StationTag start, StationTag destination, UUID playerId, boolean avoidTransfers) {
        long startTime = System.currentTimeMillis();
        UserSettings userSettings = UserSettings.getSettingsFor(playerId, true);
        NavigatableGraph graph = new NavigatableGraph(userSettings);

        List<Node> nodes = graph.searchRoute(start, destination, avoidTransfers);
        List<Route> routes = graph.searchTrainsForRoute(nodes);

        int minNumber = routes.stream().mapToInt(x -> x.getTransferCount()).min().orElse(0);
        routes = routes.stream().filter(x -> x.getTransferCount() == minNumber).toList();
        
        CreateRailwaysNavigator.LOGGER.info(String.format("%s route(s) calculated. Took %sms.",
            routes.size(),
            System.currentTimeMillis() - startTime
        ));
        return routes.stream().sorted((a, b) -> Long.compare(a.getStart().getScheduledDepartureTime(), b.getStart().getScheduledDepartureTime())).toList();
    }

    //#endregion
}
