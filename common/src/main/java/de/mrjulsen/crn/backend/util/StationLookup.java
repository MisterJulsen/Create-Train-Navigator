package de.mrjulsen.crn.backend.util;

import java.util.HashMap;
import java.util.Map;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.util.ModUtils;

/**
 * Station name lookups used by the backend's hot paths.
 * <p>
 * Both answers here are asked for constantly - resolving the display name of a single stop tests
 * station existence up to three times, and every station named anywhere in the API resolves its tag -
 * while the underlying data changes rarely. Computing them per call meant scanning every tag and
 * wildcard-matching along the way, which does not survive a network with hundreds of tags and a
 * thousand trains.
 *
 * <h2>Threading</h2>
 * Safe to call from any thread. The index is published as a whole through a volatile field, so a
 * reader sees either the previous or the rebuilt one, never a half-filled map. Two threads may
 * rebuild it at the same time, which wastes a little work but cannot produce a wrong answer.
 */
public final class StationLookup {

    /** Maps every station named by a tag to that tag, valid for the modification count it was built at. */
    private record TagIndex(long modificationCount, Map<String, StationTag> byStation) {}

    private static volatile TagIndex tagIndex = new TagIndex(-1, Map.of());

    private StationLookup() {}

    /** Whether a station with exactly this name exists in the track network. */
    public static boolean exists(String stationName) {
        return stationName != null && !stationName.isBlank() && TrainUtils.getAllStationNames().contains(stationName);
    }

    /**
     * The station tag the given station belongs to, or {@code null} if it carries none. A station in
     * several tags reports the first one that contains it.
     * <p>
     * A concrete station name - which is what practically every caller passes - is answered from the
     * index in constant time. Only a name that is itself a wildcard filter falls back to asking every
     * tag, since that is a pattern match against each tag's entries and cannot be indexed.
     */
    public static StationTag findTag(String stationName) {
        if (stationName == null || stationName.isBlank()) {
            return null;
        }

        if (ModUtils.isGlobPattern(stationName)) {
            for (StationTag tag : GlobalSettings.getInstance().getAllStationTags()) {
                if (tag.contains(stationName)) {
                    return tag;
                }
            }
            return null;
        }

        return currentIndex().byStation().get(stationName);
    }

    /** The index, rebuilt if the station tags have changed since it was built. */
    private static TagIndex currentIndex() {
        // Read before building, stored after, so a change landing mid-build is not cached away.
        long version = StationTag.getModificationCount();
        TagIndex current = tagIndex;
        if (current.modificationCount() == version) {
            return current;
        }

        Map<String, StationTag> byStation = new HashMap<>();
        for (StationTag tag : GlobalSettings.getInstance().getAllStationTags()) {
            for (String station : tag.getAllStationNames()) {
                byStation.putIfAbsent(station, tag);
            }
        }

        TagIndex rebuilt = new TagIndex(version, Map.copyOf(byStation));
        tagIndex = rebuilt;
        return rebuilt;
    }

    /** Discards the cached index, forcing the next lookup to rebuild it. */
    public static void invalidate() {
        tagIndex = new TagIndex(-1, Map.of());
    }
}
