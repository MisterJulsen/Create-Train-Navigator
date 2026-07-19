package de.mrjulsen.crn.backend.schedule;

import java.util.Optional;
import java.util.function.Function;

import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.backend.util.StationLookup;
import de.mrjulsen.crn.data.TrainLine;

/**
 * Resolves the texts shown to travellers: which station name to print for a stop, and which
 * terminus to advertise as the train's destination.
 * <p>
 * A stop's station is not always what its filter says. An ambiguous destination only resolves to a
 * concrete station once the train commits to one, and until then the best available answer is what
 * the stop resolved to on previous visits.
 */
public final class JourneyDisplayNames {

    private JourneyDisplayNames() {}

    /**
     * The station name to display for the given stop: the filter if it already names an existing
     * station, otherwise the live-resolved target, otherwise the most likely station learned from
     * past visits, falling back to the raw filter.
     */
    public static String stationName(JourneyStop stop, StopTimings timings) {
        String filter = stop.getStationFilter();
        if (StationLookup.exists(filter)) {
            return filter;
        }
        String live = stop.getStationName();
        if (live != null && !live.equals(filter) && StationLookup.exists(live)) {
            return live;
        }
        String guess = timings == null ? null : timings.getEstimatedStationName();
        if (StationLookup.exists(guess)) {
            return guess;
        }
        return live != null && !live.isBlank() ? live : filter;
    }

    /**
     * The terminus shown as the train's destination at the given stop. At the last stop of a section
     * that does not carry its start over, the train is already bound for the following section, so
     * that section's terminus is shown instead.
     */
    public static String sectionDestination(TrainJourney journey, JourneyStop stop, Function<JourneyStop, StopTimings> timings) {
        JourneySection section = stop.getSection();
        if (section == null) {
            return "";
        }
        JourneySection destinationSection = section.isLastStop(stop) && !section.includesNextSectionStart()
            ? journey.getNextSection(section)
            : section;
        return terminusStop(journey, destinationSection)
            .map(terminus -> stationName(terminus, timings.apply(terminus)))
            .orElse("");
    }

    /**
     * The terminus stop of a section: the following section's first stop if this one carries its
     * start over, otherwise this section's own last stop.
     */
    private static Optional<JourneyStop> terminusStop(TrainJourney journey, JourneySection section) {
        if (section.includesNextSectionStart()) {
            JourneySection next = journey.getNextSection(section);
            if (next != section) {
                return next.getFirstStop().or(section::getLastStop);
            }
        }
        return section.getLastStop();
    }

    /** The line name of the given section, falling back to the train name. */
    public static String displayName(JourneySection section, String trainName) {
        return Optional.ofNullable(section)
            .flatMap(JourneySection::getTrainLine)
            .map(TrainLine::getLineName)
            .filter(name -> !name.isEmpty())
            .orElse(trainName);
    }
}
