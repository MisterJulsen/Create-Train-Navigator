package de.mrjulsen.crn.backend.api;

import java.util.Optional;

import de.mrjulsen.crn.backend.util.StationLookup;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.train.TrainUtils;

/**
 * A station as this API refers to it: its name in the track network together with the station tag
 * it belongs to, so a caller never has to look the tag up itself.
 * <p>
 * Everywhere a station appears - a stop, a board row, a section's origin or terminus - it appears as
 * this record, which makes the tag name and the platform available without a second query.
 *
 * @param name The station's name in the track network. Empty if no station is known.
 * @param tag  The station tag this station belongs to, or {@code null} if it carries none. A station
 *             in several tags reports the first one that contains it.
 */
public record StationRef(String name, StationTag tag) {

    /** No station, e.g. for a stop whose station could not be resolved. */
    public static final StationRef NONE = new StationRef("", null);

    public StationRef {
        name = name == null ? "" : name;
    }

    /** The given station with its tag resolved from the global settings. */
    public static StationRef of(String stationName) {
        if (stationName == null || stationName.isBlank()) {
            return NONE;
        }
        return new StationRef(stationName, StationLookup.findTag(stationName));
    }

    /** The given station with an already known tag, which may be {@code null}. */
    public static StationRef of(String stationName, StationTag tag) {
        return stationName == null || stationName.isBlank() ? NONE : new StationRef(stationName, tag);
    }

    /** The station tag this station belongs to, if it carries one. */
    public Optional<StationTag> stationTag() {
        return Optional.ofNullable(tag);
    }

    /** Whether this station belongs to a station tag at all. */
    public boolean hasTag() {
        return tag != null;
    }

    /** The name of this station's tag, or the station's own name if it carries none. */
    public String tagName() {
        return tag == null || tag.getTagName() == null ? name : tag.getTagName().get();
    }

    /** What this station's tag knows about it, never {@code null}. */
    public StationInfo info() {
        return tag == null ? StationInfo.empty() : tag.getInfoForStation(name);
    }

    /** The platform assigned to this station by its tag, or empty if none is known. */
    public String platform() {
        return info().platform();
    }

    /** Whether a platform is known for this station. */
    public boolean hasPlatform() {
        return info().isPlatformKnown();
    }

    /** Whether this refers to a station at all. */
    public boolean isKnown() {
        return !name.isBlank();
    }

    /** Whether this station matches the given name or filter, which may contain {@code *}. */
    public boolean matches(String stationNameOrFilter) {
        return isKnown() && TrainUtils.stationMatches(name, stationNameOrFilter);
    }

    @Override
    public String toString() {
        return name;
    }
}
