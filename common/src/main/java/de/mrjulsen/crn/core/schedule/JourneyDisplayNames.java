package de.mrjulsen.crn.core.schedule;

import java.util.Optional;
import java.util.function.Function;

import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.data.settings.TrainLine;

public final class JourneyDisplayNames {

    private JourneyDisplayNames() {}

    public static String scheduledStationName(JourneyStop stop, StopTimings timings) {
        String filter = stop.getStationFilter();
        if (StationLookup.exists(filter)) {
            return filter;
        }
        String guess = timings == null ? null : timings.getEstimatedStationName();
        return StationLookup.exists(guess) ? guess : filter;
    }

    public static String realtimeStationName(JourneyStop stop, StopTimings timings) {
        String live = stop.getStationName();
        if (StationLookup.exists(live)) {
            return live;
        }
        String scheduled = scheduledStationName(stop, timings);
        if (StationLookup.exists(scheduled)) {
            return scheduled;
        }
        return live != null && !live.isBlank() ? live : stop.getStationFilter();
    }

    public static String sectionDestination(TrainJourney journey, JourneyStop stop, Function<JourneyStop, StopTimings> timings) {
        JourneySection section = stop.getSection();
        if (section == null) {
            return "";
        }
        JourneySection destinationSection = section.isLastStop(stop) && !section.includesNextSectionStart()
            ? journey.getNextSection(section)
            : section;
        return terminusStop(journey, destinationSection)
            .map(terminus -> realtimeStationName(terminus, timings.apply(terminus)))
            .orElse("");
    }

    public static Optional<JourneyStop> terminusStop(TrainJourney journey, JourneySection section) {
        if (section.includesNextSectionStart()) {
            JourneySection next = journey.getNextSection(section);
            if (next != section) {
                return next.getFirstStop().or(section::getLastStop);
            }
        }
        return section.getLastStop();
    }

    public static String displayName(JourneySection section, String trainName) {
        return Optional.ofNullable(section)
            .flatMap(JourneySection::getTrainLine)
            .map(TrainLine::getLineName)
            .filter(name -> !name.isEmpty())
            .orElse(trainName);
    }
}
