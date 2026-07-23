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
 * A stop's station is not always what its filter says. A filter containing wildcards only resolves
 * to a concrete station once the train commits to one, and which one that is can differ from run to
 * run - a blocked platform can send the train to a station in an entirely different tag. So a stop
 * has two answers, not one: the station the timetable plans for and the station the train is
 * actually heading to, which is why both are resolved separately here.
 */
public final class JourneyDisplayNames {

    private JourneyDisplayNames() {}

    /**
     * The station the timetable plans for: the filter if it already names an existing station,
     * otherwise the most likely station learned from past visits, falling back to the raw filter.
     * <p>
     * Deliberately blind to where the train is going on this run, so that a diverted train still
     * reports what it was meant to do.
     */
    public static String scheduledStationName(JourneyStop stop, StopTimings timings) {
        String filter = stop.getStationFilter();
        if (StationLookup.exists(filter)) {
            return filter;
        }
        String guess = timings == null ? null : timings.getEstimatedStationName();
        return StationLookup.exists(guess) ? guess : filter;
    }

    /**
     * The station the train is actually heading to: the live-resolved target if the schedule has
     * already committed to one, otherwise whatever the timetable plans for.
     */
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
            .map(terminus -> realtimeStationName(terminus, timings.apply(terminus)))
            .orElse("");
    }

    /**
     * The terminus stop of a section: the following section's first stop if this one carries its
     * start over, otherwise this section's own last stop.
     * <p>
     * This is the stop advertised as where the train is going, which is why anything listing the
     * stations on the way there leaves it out.
     */
    public static Optional<JourneyStop> terminusStop(TrainJourney journey, JourneySection section) {
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
