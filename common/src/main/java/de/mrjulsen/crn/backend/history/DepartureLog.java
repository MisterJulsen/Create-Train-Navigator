package de.mrjulsen.crn.backend.history;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Records the most recent departures per station, so it can be answered when the last train of a
 * given line, category or name left a station.
 */
public final class DepartureLog {

    /** How many departures are kept per station. */
    private static final int MAX_ENTRIES_PER_STATION = 20;

    private final Map<String, Deque<DepartureLogEntry>> departuresByStation = new ConcurrentHashMap<>();

    /** Records a departure at the given station. */
    public void record(String stationName, DepartureLogEntry entry) {
        if (stationName == null || stationName.isBlank()) {
            return;
        }
        Deque<DepartureLogEntry> deque = departuresByStation.computeIfAbsent(stationName, x -> new ArrayDeque<>());
        synchronized (deque) {
            while (deque.size() >= MAX_ENTRIES_PER_STATION) {
                deque.pollFirst();
            }
            deque.addLast(entry);
        }
    }

    /** The recorded departures at the given station, oldest first. */
    public List<DepartureLogEntry> getDepartures(String stationName) {
        Deque<DepartureLogEntry> deque = departuresByStation.get(stationName);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    /** The most recent departure at the given station matching the filter. */
    public Optional<DepartureLogEntry> getLastDeparture(String stationName, Predicate<DepartureLogEntry> filter) {
        List<DepartureLogEntry> entries = getDepartures(stationName);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (filter.test(entries.get(i))) {
                return Optional.of(entries.get(i));
            }
        }
        return Optional.empty();
    }

    /** The most recent departure at the given station. */
    public Optional<DepartureLogEntry> getLastDeparture(String stationName) {
        return getLastDeparture(stationName, x -> true);
    }

    /** The most recent departure of the given line at the given station. */
    public Optional<DepartureLogEntry> getLastDepartureOfLine(String stationName, UUID lineId) {
        return getLastDeparture(stationName, x -> lineId != null && lineId.equals(x.lineId()));
    }

    /** The most recent departure of the given category at the given station. */
    public Optional<DepartureLogEntry> getLastDepartureOfCategory(String stationName, UUID categoryId) {
        return getLastDeparture(stationName, x -> categoryId != null && categoryId.equals(x.categoryId()));
    }

    /** The most recent departure of the named train at the given station. */
    public Optional<DepartureLogEntry> getLastDepartureOfTrain(String stationName, String trainName) {
        return getLastDeparture(stationName, x -> x.trainName().equals(trainName));
    }

    /** Shifts all recorded departure times by the given amount, after a world time jump. */
    public void shiftTimes(long ticks) {
        if (ticks == 0) {
            return;
        }
        for (Deque<DepartureLogEntry> deque : departuresByStation.values()) {
            synchronized (deque) {
                List<DepartureLogEntry> shifted = new ArrayList<>(deque.size());
                for (DepartureLogEntry entry : deque) {
                    shifted.add(entry.shifted(ticks));
                }
                deque.clear();
                deque.addAll(shifted);
            }
        }
    }

    /** Removes the records of all stations not in the given collection. */
    public void retainStations(Collection<String> existingStations) {
        departuresByStation.keySet().retainAll(existingStations);
    }

    /** Removes all recorded departures. */
    public void clear() {
        departuresByStation.clear();
    }

    /** The number of stations departures have been recorded for. */
    public int getStationCount() {
        return departuresByStation.size();
    }

    /** Serializes the recorded departures. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, Deque<DepartureLogEntry>> e : departuresByStation.entrySet()) {
            ListTag list = new ListTag();
            synchronized (e.getValue()) {
                for (DepartureLogEntry entry : e.getValue()) {
                    list.add(entry.toNbt());
                }
            }
            nbt.put(e.getKey(), list);
        }
        return nbt;
    }

    /** Restores persisted departures. */
    public void loadNbt(CompoundTag nbt) {
        departuresByStation.clear();
        for (String station : nbt.getAllKeys()) {
            ListTag list = nbt.getList(station, Tag.TAG_COMPOUND);
            Deque<DepartureLogEntry> deque = new ArrayDeque<>();
            for (Tag tag : list) {
                if (deque.size() >= MAX_ENTRIES_PER_STATION) break;
                deque.addLast(DepartureLogEntry.fromNbt((CompoundTag)tag));
            }
            departuresByStation.put(station, deque);
        }
    }
}
