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

import org.antlr.v4.parse.ANTLRParser.blockEntry_return;

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

    protected static void init(long updateTime) {
        GlobalTrainData.makeSnapshot(updateTime);
    }

    public static List<Route> navigateForNext(long updateTime, TrainStationAlias from, TrainStationAlias to, UserSettings filterSettings) {
        init(updateTime);
        Optional<DeparturePrediction> pred = GlobalTrainData.getInstance().getNextDepartingTrainAt(from);

        if (!pred.isPresent())
            return new ArrayList<>();
   
        List<Route> routesList = Navigation.navigate(updateTime, pred.get(), new TrainStop(from, pred.get()), to, new HashSet<>(), new HashMap<>());
        routesList.sort((a, b) -> a.compareTo(b, filterSettings));
        return filter(routesList, filterSettings);
    }

    public static List<Route> navigateForAll(long updateTime, TrainStationAlias from, TrainStationAlias to, UserSettings filterSettings) {
        init(updateTime);
        List<Route> routesList = new ArrayList<>();
        Set<SimpleTrainSchedule> checkedTrains = ConcurrentHashMap.newKeySet();       
        Set<SimpleTrainSchedule> excludedTrainSchedules = ConcurrentHashMap.newKeySet();
        Map<SimpleTrainSchedule, Collection<TrainStationAlias>> scheduleExclusionHitReasons = new ConcurrentHashMap<>();

        long startTime = System.currentTimeMillis();
        if (ModCommonConfig.PARALLEL_NAVIGATION.get()) {
            GlobalTrainData.getInstance().getDepartingTrainsAt(from).parallelStream().forEach(pred -> {
                SimpleTrainSchedule checkSchedule = GlobalTrainData.getInstance().getDirectionalSchedule(pred.getTrain());
                if (checkedTrains.parallelStream().anyMatch(x -> x.equals(checkSchedule))) {
                    return;
                }
                routesList.addAll(Navigation.navigate(updateTime, pred, new TrainStop(from, pred), to, excludedTrainSchedules, scheduleExclusionHitReasons));
                checkedTrains.add(checkSchedule);
            });
        } else {
            for (DeparturePrediction pred : GlobalTrainData.getInstance().getDepartingTrainsAt(from)) {                
                SimpleTrainSchedule checkSchedule = GlobalTrainData.getInstance().getDirectionalSchedule(pred.getTrain());
                if (checkedTrains.parallelStream().anyMatch(x -> x.equals(checkSchedule))) {
                    continue;
                }
                ModMain.LOGGER.info(pred.getTrain().name.getString() + ": " + pred.getScheduleTitle());
                routesList.addAll(Navigation.navigate(updateTime, pred, new TrainStop(from, pred), to, excludedTrainSchedules, scheduleExclusionHitReasons));
                checkedTrains.add(checkSchedule);
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ModMain.LOGGER.info(String.format("%s schedules excluded because of excluded stations.", scheduleExclusionHitReasons.size()));
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
                int minDuration = route.parallelStream().mapToInt(x -> EFilterCriteria.getDataFromRoute(filterSettings.getFilterCriteria(), x)).min().orElse(0);
                return new ArrayList<>(route.stream().filter(x -> EFilterCriteria.getDataFromRoute(filterSettings.getFilterCriteria(), x) <= minDuration).toList());
            case FIXED_AMOUNT:
                return new ArrayList<>(route.subList(0, Math.min(route.size(), filterSettings.getResultCount())));
            case ALL:
            default:
                return new ArrayList<>(route.subList(0, Math.min(route.size(), 256)));
        }
    }
    
    protected static List<Route> navigate(long updateTime, DeparturePrediction train, TrainStop from, TrainStationAlias to, Set<SimpleTrainSchedule> excludedTrainSchedules, Map<SimpleTrainSchedule, Collection<TrainStationAlias>> scheduleExclusionHitReasons) {        
        GlobalSettings blacklist = GlobalSettingsManager.getInstance().getSettingsData();
        if (blacklist.isBlacklisted(from.getStationAlias()) || blacklist.isBlacklisted(to)) {
            return new ArrayList<>();
        }
        List<Route> possibilities = new ArrayList<>();
        if (!from.getStationAlias().equals(to)) {
            navigateInternal(updateTime, blacklist, (route) -> {
                //ModMain.LOGGER.debug("Found possible route: " + route.toString());
                possibilities.add(route);
            }, new ArrayList<>(), new HashSet<>(), train, from.getStationAlias(), to, from,
            null, null, excludedTrainSchedules, new HashSet<>(), scheduleExclusionHitReasons);
        }
        return possibilities;
    }

    protected static NavigationResult navigateInternal(long updateTime, GlobalSettings settings, Consumer<Route> possibleRoutes, Collection<RoutePart> currentPath, Set<TrainStop> excludedStops, DeparturePrediction currentTrain, TrainStationAlias start, TrainStationAlias end, TrainStop routeStart, SimpleTrainSchedule pregeneratedSchedule, SimpleTrainSchedule pregeneratedDirectionalSchedule, Set<SimpleTrainSchedule> excludedTrainSchedules, Set<TrainStationAlias> usedStations, Map<SimpleTrainSchedule, Collection<TrainStationAlias>> scheduleExclusionHitReasons) {
        boolean targetReachable = false;

        if (ModCommonConfig.NAVIGATION_ITERATION_DELAY.get() > 0) {
            try { Thread.sleep(ModCommonConfig.NAVIGATION_ITERATION_DELAY.get()); } catch (InterruptedException e) {}
        }
        
        SimpleTrainSchedule thisSchedule = pregeneratedSchedule == null ? GlobalTrainData.getInstance().getTrainSimpleSchedule(currentTrain.getTrain()) : pregeneratedSchedule;

        if (!thisSchedule.hasStationAlias(routeStart.getStationAlias())) {
            return new NavigationResult(false, null);
        }

        if (settings.isBlacklisted(routeStart.getStationAlias())) {
            return new NavigationResult(false, Set.of(routeStart.getStationAlias()));
        }

        if (thisSchedule.hasStationAlias(end)) {
            RoutePart route = new RoutePart(currentTrain, routeStart, end);
            if (!route.getStartStation().identical(routeStart) && route.getStopovers().parallelStream().noneMatch(x -> excludedStops.parallelStream().anyMatch(y -> x.equals(y)))) {
                Collection<RoutePart> newPath = new ArrayList<>(currentPath);
                newPath.add(route);
                possibleRoutes.accept(new Route(newPath, updateTime));
                targetReachable = true;
            }
            
        }

        SimpleTrainSchedule directionalSchedule = pregeneratedDirectionalSchedule == null ? thisSchedule.getDirectionalSchedule() : pregeneratedDirectionalSchedule;
        Set<TrainStop> stopovers = new HashSet<>();
        Set<TrainStationAlias> stationFailResults = ConcurrentHashMap.newKeySet();

        for (TrainStop stop : directionalSchedule.getAllStops()) {

            if (stop.isStationAlias(routeStart.getStationAlias()) || stop.isStationAlias(start) || settings.isBlacklisted(stop.getStationAlias())) {
                continue;
            }
            RoutePart thisRoute = new RoutePart(currentTrain, routeStart, stop.getStationAlias());

            
            ModMain.LOGGER.info("Start corrected? " + thisRoute.getStartStation().identical(routeStart));
            if (!thisRoute.getStartStation().identical(routeStart)) {
                return new NavigationResult(targetReachable, Set.of());
            }
            
            
            //ModMain.LOGGER.info("Disp: " + thisRoute.getTrain().name.getString() + ", " + thisRoute.getStartStation().getPrediction().getTicks() + ", " + thisRoute.getEndStation().getStationAlias().getAliasName().get());
            stopovers.add(stop);

            if (stop.isStationAlias(end) && thisRoute.getStartStation() != stop) {
                return new NavigationResult(targetReachable, Set.of(stop.getStationAlias()));
            }

            Set<TrainStop> excludedStopovers = stopovers.stream().filter(x -> excludedStops.stream().anyMatch(y -> x.equals(y))).collect(Collectors.toSet());
            if (!excludedStopovers.isEmpty()) {
                return new NavigationResult(targetReachable, excludedStopovers.stream().map(x -> x.getStationAlias()).collect(Collectors.toSet()));
                // oder false statt targetReachable
            }

            if (thisRoute.getStopovers().stream().anyMatch(x -> x.getStationAlias().equals(end) || excludedStops.stream().anyMatch(y -> x.equals(y)))) {
                continue;
            }

            Map<UUID, SimpleTrainSchedule> pregeneratedTrainSchedules = new ConcurrentHashMap<>();
            
            ModMain.LOGGER.info(currentTrain.getTrain().name.getString());
            Set<DeparturePrediction> departingTrains = GlobalTrainData.getInstance().getDepartingTrainsAt(stop.getStationAlias()).stream()
            .filter(x -> {
                SimpleTrainSchedule schedule = GlobalTrainData.getInstance().getTrainSimpleSchedule(x.getTrain());
                
                /*
                ModMain.LOGGER.info("Connection: " + x.getTrain().name.getString() + ", " + x.getTicks());
                for (TrainStop sss : schedule.getAllStops()) {
                    ModMain.LOGGER.info("\t-> " + sss.getStationAlias().getAliasName().get() +  " " + sss.getPrediction().getTicks());
                }
                */
                // Er darf nicht automatisch den nächsten Halt nehmen, sondern darf diese Option

                // Es darf der Start bei RoutePart nicht korrigiert sein. Falls er korrigiert werden muss, ist diese Connections nicht gut
                boolean b = pregeneratedTrainSchedules.values().stream().noneMatch(a -> a.exactEquals(schedule));
                pregeneratedTrainSchedules.put(x.getTrain().id, schedule);

                return !x.getTrain().id.equals(currentTrain.getTrain().id) &&
                       !schedule.equals(thisSchedule) &&
                       TrainUtils.isTrainValid(x.getTrain())
                       //&& b
                ;
            }).collect(Collectors.toSet());

            List<DeparturePrediction> trainPredictions = departingTrains.stream().map(x -> {
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
            })/*.filter(x -> x.getTicks() > thisRoute.getEndStation().getPrediction().getTicks())*/.sorted(Comparator.comparingInt(x -> x.getTicks())).toList();
            
            //trainPredictions.sort(Comparator.comparingInt(x -> x.getTicks()));

            if (trainPredictions == null || trainPredictions.isEmpty()) {
                continue;
            }

            for (DeparturePrediction pred : trainPredictions) {
                //ModMain.LOGGER.info("\t-> " + pred.getTrain().name.getString() + " " + pred.getTicks());
            }

            Set<SimpleTrainSchedule> checkedTrains = ConcurrentHashMap.newKeySet();
            for (DeparturePrediction pred : trainPredictions) {                
                SimpleTrainSchedule checkSchedule = pregeneratedTrainSchedules.get(pred.getTrain().id).getDirectionalSchedule();

                if (scheduleExclusionHitReasons.getOrDefault(checkSchedule, List.of()).stream().anyMatch(x -> usedStations.stream().anyMatch(y -> y.equals(x)))) {
                    stationFailResults.addAll(scheduleExclusionHitReasons.get(checkSchedule));
                    continue;
                }

                if (checkedTrains.parallelStream().anyMatch(x -> x.equals(checkSchedule)) || excludedTrainSchedules.parallelStream().anyMatch(x -> x.equals(checkSchedule))) {
                    continue;
                }

                /*
                ModMain.LOGGER.info(pred.getTrain().name.getString());
                ModMain.LOGGER.info("Nav: " + pred.getTrain().name.getString() + ", " + pred.getTicks());
                for (TrainStop sss : pregeneratedTrainSchedules.get(pred.getTrain().id).getAllStops()) {
                    ModMain.LOGGER.info("\t-> " + sss.getStationAlias().getAliasName().get() +  " " + sss.getPrediction().getTicks());
                }
                */

                Set<TrainStop> newExcludedStops = new HashSet<>(excludedStops);
                newExcludedStops.add(new TrainStop(start, null));
                newExcludedStops.addAll(stopovers);
                newExcludedStops.addAll(thisRoute.getStopovers());
                
                Collection<RoutePart> newPath = new ArrayList<>(currentPath);
                newPath.add(thisRoute);

                Set<TrainStationAlias> usedStationsNew = new HashSet<>(usedStations);
                usedStationsNew.addAll(thisRoute.getStopovers().stream().map(x -> x.getStationAlias()).toList());

                NavigationResult b = navigateInternal(updateTime, settings, possibleRoutes, newPath, newExcludedStops, pred, start, end, thisRoute.getEndStation(), pregeneratedTrainSchedules.get(pred.getTrain().id), checkSchedule, excludedTrainSchedules, usedStationsNew, scheduleExclusionHitReasons);
                targetReachable = targetReachable || b.possible();

                if (!b.success() && b.failReasonStation() != null && !b.failReasonStation().isEmpty()) {
                    // Fail wegen einer bestimmten Station. Dieser Fahrplan geht also nicht, wenn die Station schon verbraucht ist.
                    scheduleExclusionHitReasons.put(checkSchedule, b.failReasonStation());
                }

                if (b.impossible()) {
                    excludedTrainSchedules.add(new SimpleTrainSchedule(null));
                }                
                
                if (!(!b.success() && b.failReasonStation() != null && b.failReasonStation().isEmpty())) {
                    checkedTrains.add(checkSchedule);                    
                }
            }
        }

        return new NavigationResult(targetReachable, stationFailResults);
    }
}
