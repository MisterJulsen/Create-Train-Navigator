package de.mrjulsen.crn.core.schedule;

import java.util.Optional;

import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.TrainLine;

public final class JourneyDisplayNames {

    private JourneyDisplayNames() {}

    public static String scheduledStationName(JourneyStop stop, StopTimings timings) {
        String filter = stop.getStationFilter();
        if (StationLookup.exists(filter)) {
            return filter;
        }
        String guess = timings == null ? null : timings.getEstimatedStationName();
        return StationLookup.exists(guess) ? guess : "";
    }

    public static String realtimeStationName(JourneyStop stop, StopTimings timings) {
        String live = stop.getStationName();
        if (stop.isStationResolved() && StationLookup.exists(live)) {
            return live;
        }
        String scheduled = scheduledStationName(stop, timings);
        if (StationLookup.exists(scheduled)) {
            return scheduled;
        }
        if (StationLookup.exists(live)) {
            return live;
        }
        return live != null && !live.isBlank() ? live : stop.getStationFilter();
    }

    public static String sectionDestination(TrainJourney journey, JourneyStop stop, TrackedTrain train) {
        JourneySection section = stop.getSection();
        if (section == null) {
            return "";
        }
        JourneySection destinationSection = section.isLastStop(stop) && !section.includesNextSectionStart() ? journey.nextSectionOf(section).orElse(section) : section;
        return terminusStop(journey, destinationSection, train)
            .map(train::getDisplayStationName)
            .orElse("");
    }

    public static Optional<JourneyStop> terminusStop(TrainJourney journey, JourneySection section, TrackedTrain train) {
        if (section.includesNextSectionStart()) {
            return journey.nextSectionOf(section)
                .flatMap(sec -> getFirstPublicStop(train, sec))
                .or(() -> getLastPublicStop(train, section));
        }
        return getLastPublicStop(train, section);
    }

    public static String displayName(JourneySection section, String trainName) {
        return Optional.ofNullable(section)
            .flatMap(JourneySection::getTrainLine)
            .map(TrainLine::getLineName)
            .filter(name -> !name.isEmpty())
            .orElse(trainName);
    }

    public static boolean isPublic(TrackedTrain train, JourneyStop stop) {
        return !GlobalSettings.getInstance().isStationBlacklisted(train.getDisplayStationName(stop));
    }

    public static Optional<JourneyStop> getFirstPublicStop(TrackedTrain train, JourneySection section) {
        for (JourneyStop stop : section.getStops()) {
            if (isPublic(train, stop)) {
                return Optional.of(stop);
            }
        }
        return Optional.empty();
    }

    public static Optional<JourneyStop> getLastPublicStop(TrackedTrain train, JourneySection section) {
        JourneyStop stop;
        for (int i = section.getStops().size() - 1; i >= 0; i--) {
            stop = section.getStops().get(i);
            if (isPublic(train, stop)) {
                return Optional.of(stop);
            }
        }
        return Optional.empty();
    }

    public static boolean isPublicTerminus(TrackedTrain train, TrainJourney journey, JourneyStop stop) {
        if (journey.isTerminus(stop)) {
            return true;
        }
        JourneySection section = stop.getSection();
        if (section == null || journey.carriesPassengersOnward(section)) {
            return false;
        }
        return getLastPublicStop(train, section).map(x -> x == stop).orElse(false);
    }

    public static boolean isPublicOrigin(TrackedTrain train, TrainJourney journey, JourneyStop stop) {
        if (journey.isOrigin(stop)) {
            return true;
        }
        JourneySection section = stop.getSection();
        if (section == null || !section.isUsable()) {
            return false;
        }
        boolean carriedIn = journey.previousSectionOf(section)
            .map(previous -> previous.isUsable() && journey.carriesPassengersOnward(previous))
            .orElse(false);
        if (carriedIn) {
            return false;
        }
        return getFirstPublicStop(train, section).map(x -> x == stop).orElse(false);
    }

    public static Optional<JourneyStop> getNextPublicStop(TrainJourney journey, JourneyStop stop, TrackedTrain train) {
        JourneyStop current = stop;
        for (int i = 0; i < journey.getStopCount(); i++) {
            Optional<JourneyStop> next = journey.getNextStop(current);
            if (next.isEmpty()) {
                return Optional.empty();
            }
            current = next.get();
            if (current == stop) {
                return Optional.empty();
            }
            if (isPublic(train, current)) {
                return Optional.of(current);
            }
        }
        return Optional.empty();
    }

}
