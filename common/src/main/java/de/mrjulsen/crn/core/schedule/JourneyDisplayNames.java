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
            ? journey.nextSectionOf(section).orElse(section)
            : section;
        return terminusStop(journey, destinationSection)
            .map(terminus -> realtimeStationName(terminus, timings.apply(terminus)))
            .orElse("");
    }

    /**
     * The stop a section's travellers ride to. Where the section carries them onward, that is the
     * first stop of the following section, which on a cyclic run with a single section is the
     * section's own first stop again.
     */
    public static Optional<JourneyStop> terminusStop(TrainJourney journey, JourneySection section) {
        if (section.includesNextSectionStart()) {
            return journey.nextSectionOf(section)
                .flatMap(JourneySection::getFirstStop)
                .or(section::getLastStop);
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
