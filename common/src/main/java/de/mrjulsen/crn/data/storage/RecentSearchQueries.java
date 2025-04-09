package de.mrjulsen.crn.data.storage;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class RecentSearchQueries {

    private static final int MAX = 5;

    public static class RecentSearchQuery {

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
    }

    private static final String NBT_QUERIES = "Queries";

    private final List<RecentSearchQuery> queries = new LinkedList<>();

    public void add(RecentSearchQuery query) {        
        for (int i = 0; i < queries.size(); i++) {
            if (queries.get(i).equals(query)) {
                queries.remove(i);
            }
        }
        while (queries.size() >= MAX) {
            queries.remove(0);
        }
        queries.add(query);
    }

    public RecentSearchQuery[] getAll() {
        return queries.toArray(RecentSearchQuery[]::new);
    }

    public void clear() {
        queries.clear();
    }

    public boolean isEmpty() {
        return queries.isEmpty();
    }

    public RecentSearchQuery get(int i) {
        return queries.get(i);
    }

    public int size() {
        return queries.size();
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (RecentSearchQuery query : queries) {
            list.add(query.toNbt());
        }
        nbt.put(NBT_QUERIES, list);
        return nbt;
    }

    public static RecentSearchQueries fromNbt(CompoundTag nbt) {
        RecentSearchQueries queries = new RecentSearchQueries();
        queries.queries.addAll(nbt.getList(NBT_QUERIES, Tag.TAG_COMPOUND).stream().map(x -> RecentSearchQuery.fromNbt((CompoundTag)x)).toList());
        return queries;
    }

    public void remove(RecentSearchQuery query) {
        queries.removeIf(query::equals);
    }
}