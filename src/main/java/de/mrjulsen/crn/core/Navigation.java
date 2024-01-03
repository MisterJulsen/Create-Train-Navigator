package de.mrjulsen.crn.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.EFilterCriteria;
import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.GlobalTrainData;
import de.mrjulsen.crn.data.Route;
import de.mrjulsen.crn.data.RoutePart;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.util.TrainUtils;

public class Navigation {

    protected static void init() {
        GlobalTrainData.makeSnapshot();
    }

    public static List<Route> navigateForNext(TrainStationAlias from, TrainStationAlias to, UserSettings filterSettings) {
        init();
        Optional<DeparturePrediction> pred = GlobalTrainData.getInstance().getNextDepartingTrainAt(from);

        if (!pred.isPresent())
            return new ArrayList<>();
   
        List<Route> routesList = Navigation.navigate(pred.get(), new TrainStop(from, pred.get()), to);
        routesList.sort((a, b) -> a.compareTo(b, filterSettings));
        return filter(routesList, filterSettings);
    }

    public static List<Route> navigateForAll(TrainStationAlias from, TrainStationAlias to, UserSettings filterSettings) {
        init();
        List<Route> routesList = new ArrayList<>();
        Collection<SimpleTrainSchedule> checkedTrains = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        for (DeparturePrediction pred : GlobalTrainData.getInstance().getDepartingTrainsAt(from)) {                

            SimpleTrainSchedule checkSchedule = GlobalTrainData.getInstance().getDirectionalSchedule(pred.getTrain());
            if (checkedTrains.stream().anyMatch(x -> x.equals(checkSchedule))) {
                continue;
            }
            routesList.addAll(Navigation.navigate(pred, new TrainStop(from, pred), to));
            checkedTrains.add(checkSchedule);
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ModMain.LOGGER.info(String.format("Navigation succeeded. Took %sms. Found %s possible routes.", estimatedTime, routesList.size()));
        routesList.sort((a, b) -> a.compareTo(b, filterSettings));
        return filter(routesList, filterSettings);
    }

    protected static List<Route> filter(List<Route> route, UserSettings filterSettings) {
        if (route == null || route.size() <= 0) {
            return route;
        }

        switch (filterSettings.getResultType()) {
            case BEST:
                int minDuration = route.stream().mapToInt(x -> EFilterCriteria.getDataFromRoute(filterSettings.getFilterCriteria(), x)).min().orElse(0);
                return new ArrayList<>(route.stream().filter(x -> EFilterCriteria.getDataFromRoute(filterSettings.getFilterCriteria(), x) <= minDuration).toList());
            case FIXED_AMOUNT:
                return new ArrayList<>(route.subList(0, Math.min(route.size(), filterSettings.getResultCount())));
            case ALL:
            default:
                return route;
        }
    }
    
    protected static List<Route> navigate(DeparturePrediction train, TrainStop from, TrainStationAlias to) {        
        GlobalSettings blacklist = GlobalSettingsManager.getInstance().getSettingsData();
        if (blacklist.isBlacklisted(from.getStationAlias()) || blacklist.isBlacklisted(to)) {
            return new ArrayList<>();
        }

        List<Route> possibilities = new ArrayList<>();
        if (!from.getStationAlias().equals(to)) {
            navigateInternal(blacklist, (route) -> {
                ModMain.LOGGER.debug("Found possible route: " + route.toString());
                possibilities.add(route);
            }, new ArrayList<>(), new ArrayList<>(), train, from.getStationAlias(), to, from,
            null);
        }
        return possibilities;
    }
    
    protected static void navigateInternal(GlobalSettings settings, Consumer<Route> possibleRoutes, Collection<RoutePart> currentPath, Collection<TrainStop> excludedStops, DeparturePrediction currentTrain, TrainStationAlias start, TrainStationAlias end, TrainStop routeStart, SimpleTrainSchedule pregeneratedSchedule) {
        
        if (ModCommonConfig.NAVIGATION_ITERATION_DELAY.get() > 0) {
            try { Thread.sleep(ModCommonConfig.NAVIGATION_ITERATION_DELAY.get()); } catch (InterruptedException e) {}
        }
        
        SimpleTrainSchedule thisSchedule = pregeneratedSchedule == null ? GlobalTrainData.getInstance().getTrainSimpleSchedule(currentTrain.getTrain()) : pregeneratedSchedule;

        if (!thisSchedule.hasStationAlias(routeStart.getStationAlias())) {
            return;
        }

        if (settings.isBlacklisted(routeStart.getStationAlias())) {
            return;
        }

        if (thisSchedule.hasStationAlias(end)) {
            RoutePart route = new RoutePart(currentTrain, routeStart, end);
            if (route.getStopovers().stream().noneMatch(x -> excludedStops.stream().anyMatch(y -> x.equals(y)))) {
                Collection<RoutePart> newPath = new ArrayList<>();
                newPath.addAll(currentPath);
                newPath.add(route);
                possibleRoutes.accept(new Route(newPath));
            }
            
        }

        SimpleTrainSchedule directionalSchedule = thisSchedule.getDirectionalSchedule();
        Collection<TrainStop> stopovers = new ArrayList<>();

        for (TrainStop stop : directionalSchedule.getAllStops()) {

            if (stop.isStationAlias(routeStart.getStationAlias()) || stop.isStationAlias(start)) {
                continue;
            }

            if (settings.isBlacklisted(stop.getStationAlias())) {
                continue;
            }

            RoutePart thisRoute = new RoutePart(currentTrain, routeStart, stop.getStationAlias());            
            stopovers.add(stop);

            if (stop.isStationAlias(end) || stopovers.stream().anyMatch(x -> excludedStops.stream().anyMatch(y -> x.equals(y)))) {
                return;
            }
            
            if (thisRoute.getStopovers().stream().anyMatch(x -> x.getStationAlias().equals(end) || excludedStops.stream().anyMatch(y -> x.equals(y)))) {
                continue;
            }

            Map<UUID, SimpleTrainSchedule> pregeneratedTrainSchedules = new ConcurrentHashMap<>();
            
            Collection<DeparturePrediction> departingTrains = GlobalTrainData.getInstance().getDepartingTrainsAt(stop.getStationAlias()).stream()
            .filter(x -> {
                SimpleTrainSchedule schedule = GlobalTrainData.getInstance().getTrainSimpleSchedule(x.getTrain());
                pregeneratedTrainSchedules.put(x.getTrain().id, schedule);

                return !x.getTrain().id.equals(currentTrain.getTrain().id) &&
                       !schedule.equals(thisSchedule) &&
                       TrainUtils.isTrainValid(x.getTrain())
                ;
            }).collect(Collectors.toList());

            Collection<DeparturePrediction> trainPredictions = departingTrains.stream().map(x -> {
                TrainStop nextStop = thisRoute.getEndStation();
                if (x.getTicks() < nextStop.getPrediction().getTicks()) {
                    int trainCycleDuration = DeparturePrediction.getTrainCycleDuration(x.getTrain());
                    if (trainCycleDuration > 0) {
                        int diffTicks = nextStop.getPrediction().getTicks() - x.getTicks();
                        int mul = diffTicks / trainCycleDuration + 1;

                        return DeparturePrediction.withCycleTicks(x, mul);
                    }
                }
                return x;
            }).filter(x -> x.getTicks() > thisRoute.getEndStation().getPrediction().getTicks()).sorted(Comparator.comparingInt(x -> x.getTicks())).toList(); 

            if (trainPredictions == null || trainPredictions.size() <= 0) {
                continue;
            }

            Collection<SimpleTrainSchedule> checkedTrains = new ArrayList<>();            
            for (DeparturePrediction pred : trainPredictions) {                

                SimpleTrainSchedule checkSchedule = GlobalTrainData.getInstance().getDirectionalSchedule(pred.getTrain());
                if (checkedTrains.stream().anyMatch(x -> x.equals(checkSchedule))) {
                    continue;
                }

                Collection<TrainStop> newExcludedStops = new ArrayList<>();
                newExcludedStops.add(new TrainStop(start, null));
                newExcludedStops.addAll(excludedStops);
                newExcludedStops.addAll(stopovers);
                newExcludedStops.addAll(thisRoute.getStopovers());
                newExcludedStops = newExcludedStops.stream().distinct().toList();
                
                Collection<RoutePart> newPath = new ArrayList<>();
                newPath.addAll(currentPath);
                newPath.add(thisRoute);

                navigateInternal(settings, possibleRoutes, newPath, newExcludedStops, pred, start, end, thisRoute.getEndStation(), pregeneratedTrainSchedules.get(pred.getTrain().id));
                
                checkedTrains.add(checkSchedule);
            }
        }
    }
}
