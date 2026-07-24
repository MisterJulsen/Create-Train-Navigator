package de.mrjulsen.crn.core.util;

import java.util.HashMap;
import java.util.Map;

import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.util.ModUtils;

public final class StationLookup {

    private record TagIndex(long modificationCount, Map<String, StationTag> byStation) {}

    private static volatile TagIndex tagIndex = new TagIndex(-1, Map.of());

    private StationLookup() {}

    public static boolean exists(String stationName) {
        return stationName != null && !stationName.isBlank() && TrainUtils.getAllStationNames().contains(stationName);
    }

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

    private static TagIndex currentIndex() {
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

    public static void invalidate() {
        tagIndex = new TagIndex(-1, Map.of());
    }
}
