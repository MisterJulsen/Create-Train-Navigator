package de.mrjulsen.crn.data.settings;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public class RecentSearchQueries {

    public static final int MAX = 20;
    public static final int MAX_PINS = 3;

    public static class RecentSearchQuery implements Comparable<RecentSearchQuery> {

        private static final String NBT_START = "Start";
        private static final String NBT_END = "End";
        private static final String NBT_CREATION_TIME = "creationTime";

        private final String startStation;
        private final String destinationStation;
        private final long time;

        private RecentSearchQuery(String startStation, String destinationStation, long time) {
            this.startStation = startStation;
            this.destinationStation = destinationStation;
            this.time = time;
        }

        public RecentSearchQuery(String startStation, String destinationStation) {
            this(startStation, destinationStation, new Date().getTime());
        }

        public String getStartStation() {
            return startStation;
        }

        public String getDestinationStation() {
            return destinationStation;
        }

        public long getCreationTime() {
            return time;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_START, startStation);
            nbt.putString(NBT_END, destinationStation);
            nbt.putLong(NBT_CREATION_TIME, time);
            return nbt;
        }

        public static RecentSearchQuery fromNbt(CompoundTag nbt) {
            return new RecentSearchQuery(
                nbt.getString(NBT_START),
                nbt.getString(NBT_END),
                nbt.getLong(NBT_CREATION_TIME)
            );
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RecentSearchQuery o) {
                return startStation.equals(o.startStation) && destinationStation.equals(o.destinationStation);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startStation, destinationStation);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", startStation, destinationStation);
        }

        @Override
        public int compareTo(@NotNull RecentSearchQueries.RecentSearchQuery o) {
            return Long.compare(time, o.time) * -1;
        }
    }

    private static final String NBT_QUERIES = "Queries";
    private static final String NBT_PINNED = "Pinned";

    private final List<RecentSearchQuery> queries = new LinkedList<>();
    private final List<RecentSearchQuery> pinned = new LinkedList<>();

    public void add(RecentSearchQuery query) {
        boolean isPinned = false;
        for (int i = 0; i < pinned.size(); i++) {
            if (pinned.get(i).equals(query)) {
                pinned.remove(i);
                isPinned = true;
            }
        }
        for (int i = 0; i < queries.size(); i++) {
            if (queries.get(i).equals(query)) {
                queries.remove(i);
            }
        }

        if (isPinned) {
            pinned.add(query);
        } else {
            while (queries.size() >= MAX) {
                queries.remove(0);
            }
            queries.add(query);
        }
        sort();
    }

    public RecentSearchQuery[] getAll() {
        return queries.toArray(RecentSearchQuery[]::new);
    }

    public RecentSearchQuery[] getAllPins() {
        return pinned.toArray(RecentSearchQuery[]::new);
    }

    public boolean isPinned(RecentSearchQuery query) {
        return pinned.contains(query);
    }

    public void clearQueries() {
        queries.clear();
    }

    public void clearPins() {
        queries.clear();
    }

    public void clearAll() {
        clearPins();
        clearQueries();
    }

    public boolean isEmpty() {
        return queries.isEmpty();
    }

    public boolean hasPins() {
        return !pinned.isEmpty();
    }

    public RecentSearchQuery get(int i) {
        return queries.get(i);
    }

    public RecentSearchQuery getPinned(int i) {
        return pinned.get(i);
    }

    public int queriesSize() {
        return queries.size();
    }

    public int pinnedSize() {
        return pinned.size();
    }

    private void sort() {
        queries.sort(RecentSearchQuery::compareTo);
        pinned.sort(RecentSearchQuery::compareTo);
    }

    public boolean pin(RecentSearchQuery query) {
        if (!queries.contains(query) || pinned.contains(query) || pinned.size() >= MAX_PINS) {
            return false;
        }
        queries.remove(query);
        pinned.add(query);
        sort();
        return true;
    }

    public boolean unpin(RecentSearchQuery query) {
        if (!pinned.contains(query) || queries.contains(query)) {
            return false;
        }
        pinned.remove(query);
        queries.add(query);
        sort();
        return true;
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag queryList = new ListTag();
        for (RecentSearchQuery query : queries) {
            queryList.add(query.toNbt());
        }
        nbt.put(NBT_QUERIES, queryList);

        ListTag pinList = new ListTag();
        for (RecentSearchQuery query : pinned) {
            pinList.add(query.toNbt());
        }
        nbt.put(NBT_PINNED, pinList);
        return nbt;
    }

    public static RecentSearchQueries fromNbt(CompoundTag nbt) {
        RecentSearchQueries queries = new RecentSearchQueries();
        queries.queries.addAll(nbt.getList(NBT_QUERIES, Tag.TAG_COMPOUND).stream().map(x -> RecentSearchQuery.fromNbt((CompoundTag)x)).limit(MAX).toList());
        queries.pinned.addAll(nbt.getList(NBT_PINNED, Tag.TAG_COMPOUND).stream().map(x -> RecentSearchQuery.fromNbt((CompoundTag)x)).limit(MAX_PINS).toList());
        queries.sort();
        return queries;
    }

    public void remove(RecentSearchQuery query) {
        queries.removeIf(query::equals);
        pinned.removeIf(query::equals);
        sort();
    }
}