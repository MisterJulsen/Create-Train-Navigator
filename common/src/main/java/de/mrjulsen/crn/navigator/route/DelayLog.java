package de.mrjulsen.crn.navigator.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * Every reason a leg of a route was ever delayed, in the order they first appeared.
 * <p>
 * This is a log, not a mirror. The train reports the reasons that apply at this moment, and a view
 * that simply showed those would have nothing left to show the moment the train made its time back
 * up - even though the stops it reached late still stand there in red. So a reason that goes away is
 * {@linkplain RecordedDelay#closedAt(long) closed} rather than removed, and everything collected
 * stays for as long as the route does.
 * <p>
 * A reason is identified by its cause and the time it started, so the same occurrence being reported
 * again refreshes the entry it already has instead of adding another. Once the leg has been
 * travelled the log is {@linkplain #freeze(long) frozen}: what the train gets up to on its next run
 * is a different journey and no longer this one's history.
 */
public final class DelayLog {

    private final Map<String, RecordedDelay> entries = new LinkedHashMap<>();
    private boolean frozen;

    private static final String NBT_ENTRIES = "Entries";
    private static final String NBT_FROZEN = "Frozen";

    /**
     * Takes in the reasons the train reports now: new ones are added, known ones refreshed, and ones
     * that have gone are closed at the given time.
     */
    public void update(Collection<DelayInstance> reported, long now) {
        if (frozen) {
            return;
        }
        Set<String> stillApplying = new HashSet<>();
        for (DelayInstance delay : reported) {
            String key = key(delay);
            stillApplying.add(key);
            entries.put(key, RecordedDelay.active(delay));
        }
        for (Map.Entry<String, RecordedDelay> entry : entries.entrySet()) {
            if (entry.getValue().isActive() && !stillApplying.contains(entry.getKey())) {
                entry.setValue(entry.getValue().closedAt(now));
            }
        }
    }

    /** Closes whatever still applies and stops accepting anything further. Cannot be undone. */
    public void freeze(long now) {
        if (frozen) {
            return;
        }
        frozen = true;
        entries.replaceAll((key, entry) -> entry.closedAt(now));
    }

    /** Whether this log has been sealed because its leg has been travelled. */
    public boolean isFrozen() {
        return frozen;
    }

    /** Every reason collected, in the order they first appeared. */
    public List<RecordedDelay> entries() {
        return List.copyOf(entries.values());
    }

    /** The occurrences behind {@link #entries()}, one per entry. */
    public List<DelayInstance> instances() {
        return entries.values().stream().map(RecordedDelay::instance).toList();
    }

    /**
     * The reasons to show, one line per kind: the same reason recurring over the course of a leg is
     * one thing to tell the traveller about, not several. Use {@link #entries()} where the individual
     * occurrences and their durations matter.
     */
    public List<DelayInstance> reasons() {
        return DelayInstance.collapseByCause(instances());
    }

    /** Only the reasons that still apply. */
    public List<RecordedDelay> active() {
        return entries.values().stream().filter(RecordedDelay::isActive).toList();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /** Serializes this log. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_ENTRIES, NbtHelper.writeList(new ArrayList<>(entries.values()), RecordedDelay::toNbt));
        nbt.putBoolean(NBT_FROZEN, frozen);
        return nbt;
    }

    /** Deserializes a log written by {@link #toNbt()}. */
    public static DelayLog fromNbt(CompoundTag nbt) {
        DelayLog log = new DelayLog();
        for (RecordedDelay entry : NbtHelper.readList(nbt, NBT_ENTRIES, RecordedDelay::fromNbt)) {
            log.entries.put(key(entry.instance()), entry);
        }
        log.frozen = nbt.getBoolean(NBT_FROZEN);
        return log;
    }

    private static String key(DelayInstance delay) {
        return delay.causeId() + "@" + delay.since();
    }

    @Override
    public String toString() {
        return entries.size() + " delay reasons" + (frozen ? " (frozen)" : "");
    }
}
