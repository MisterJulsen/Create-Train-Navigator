package de.mrjulsen.crn.data.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.CategoryRef;
import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.LineRef;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.api.SectionSnapshot;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainState;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.NavigationResult;
import de.mrjulsen.crn.navigator.Navigator;
import de.mrjulsen.crn.navigator.RouteOptimization;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.util.ModUtils;

public final class NavigatorRoutes {

    private NavigatorRoutes() {}

    public static List<Route> search(StationTag start, StationTag destination, UUID playerId, boolean avoidTransfers) {
        UserSettings settings = UserSettings.getSettingsFor(playerId, true);

        NavigationQuery query = NavigationQuery.from(tagName(start))
            .to(tagName(destination))
            .departingIn(settings.navigationDepartureInTicks.getValue())
            .withMinTransferTime(settings.navigationTransferTime.getValue())
            .excludingCategories(settings.navigationExcludedTrainCategories.getValue())
            .preferring(avoidTransfers ? RouteOptimization.FEWEST_TRANSFERS : RouteOptimization.FASTEST);

        NavigationResult result = Navigator.search(query);
        if (ModCommonConfig.ADVANCED_LOGGING.get()) {
            CreateRailwaysNavigator.LOGGER.info(String.format("%s route(s) calculated. Took %sms. Searched %s nodes, %s trips.",
                result.size(), result.durationMs(), result.stationsSearched(), result.tripsScanned()));
        }

        List<Route> routes = new ArrayList<>(result.size());
        for (RouteJourney journey : result.journeys()) {
            Route route = toRoute(journey);
            if (route != null) {
                routes.add(route);
            }
        }
        routes.sort(Comparator.comparingLong(x -> x.getStart().getScheduledDepartureTime()));
        return routes;
    }

    /**
     * The configured line an API value refers to. The legacy route format holds the live objects,
     * so building one has to look them back up.
     */
    private static TrainLine resolveLine(LineRef line) {
        return line == null || !line.isKnown() ? null : GlobalSettings.getInstance().getTrainLine(line.id()).orElse(null);
    }

    /** The configured category an API value refers to. */
    private static TrainCategory resolveCategory(CategoryRef category) {
        return category == null || !category.isKnown() ? null : GlobalSettings.getInstance().getTrainCategory(category.id()).orElse(null);
    }

    private static String tagName(StationTag tag) {
        return tag == null || tag.getTagName() == null ? "" : tag.getTagName().get();
    }

    private static Route toRoute(RouteJourney journey) {
        Map<UUID, JourneySnapshot> journeys = new HashMap<>();
        List<RoutePart> parts = new ArrayList<>(journey.legs().size());

        for (RouteLeg leg : journey.legs()) {
            TrainSnapshot train = RailwayBackendApi.getTrain(leg.trainId()).orElse(null);
            if (train == null) {
                return null;
            }
            JourneySnapshot snapshot = journeys.computeIfAbsent(leg.trainId(),
                x -> RailwayBackendApi.getJourney(x).orElse(null));

            List<TrainStop> stops = new ArrayList<>(leg.calls().size());
            for (RouteCall call : leg.calls()) {
                stops.add(toTrainStop(train, leg, call));
            }
            parts.add(new RoutePart(leg.sessionId(), leg.trainId(), stops, sectionStops(train, leg, snapshot)));
        }

        return parts.isEmpty() ? null : new Route(parts, false);
    }

    private static List<TrainStop> sectionStops(TrainSnapshot train, RouteLeg leg, JourneySnapshot journey) {
        if (journey == null) {
            return List.of();
        }

        SectionSnapshot section = journey.sections().stream()
            .filter(x -> x.index() == leg.sectionIndex())
            .findFirst()
            .orElse(null);
        if (section == null) {
            return List.of();
        }

        RouteCall boarding = leg.boarding();
        StopSnapshot anchor = null;
        for (StopSnapshot stop : section.stops()) {
            if (stop.entryIndex() == boarding.entryIndex() && stop.hasTimes()) {
                anchor = stop;
                break;
            }
        }

        long shift = anchor != null && anchor.scheduled().isKnown() && boarding.scheduled().isKnown()
            ? boarding.scheduled().arrival() - anchor.scheduled().arrival()
            : 0;
        int cycles = anchor == null ? 0 : boarding.cycle() - anchor.completedVisits();

        List<TrainStop> stops = new ArrayList<>(section.stops().size());
        for (StopSnapshot stop : section.stops()) {
            if (!stop.hasTimes()) {
                continue;
            }
            stops.add(toTrainStop(train, leg, new RouteCall(
                stop.scheduledStation(),
                stop.realtimeStation(),
                stop.entryIndex(),
                stop.completedVisits() + cycles,
                stop.scheduled().shifted(shift),
                stop.realtime().shifted(shift)
            )));
        }
        return stops;
    }

    private static TrainStop toTrainStop(TrainSnapshot train, RouteLeg leg, RouteCall call) {
        StopTimes scheduled = call.scheduled().isKnown() ? call.scheduled() : call.realtime();
        StationTag scheduledTag = GlobalSettings.getInstance().getOrCreateStationTagFor(call.scheduledStationName());
        StationTag realTimeTag = GlobalSettings.getInstance().getOrCreateStationTagFor(call.realtimeStationName());

        return new TrainStop(
            call.entryIndex(),
            leg.sectionIndex(),
            leg.trainId(),
            train.trainName(),
            iconOf(train),
            new TrainInfo(resolveLine(leg.line()), resolveCategory(leg.category())),
            leg.destinationText(),
            false,
            leg.destinationText(),
            (int) scheduled.stayDuration(),
            call.cycle() > 0,
            scheduled.departure(),
            scheduled.arrival(),
            call.cycle(),
            scheduledTag.getClientTag(call.scheduledStationName()),
            call.realtime().arrival(),
            call.realtime().departure(),
            call.cycle(),
            realTimeTag.getClientTag(call.realtimeStationName()),
            (int) (call.realtime().arrival() - ModUtils.getTransformedWorldTime()),
            TrainState.BEFORE
        );
    }

    private static TrainIconType iconOf(TrainSnapshot train) {
        return train.iconId() == null ? TrainIconType.getDefault() : TrainIconType.byId(train.iconId());
    }
}
