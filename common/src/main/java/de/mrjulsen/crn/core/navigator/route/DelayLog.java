package de.mrjulsen.crn.core.navigator.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * The delay reasons recorded against a route leg over its life, kept even once they no longer apply
 * so that a journey's history stays complete. The log is filled while the journey is tracked and
 * frozen once the leg has been travelled.
 */
public final class DelayLog {

    private final Map<String, RecordedDelay> entries = new LinkedHashMap<>();
    private boolean frozen;

    private static final String NBT_ENTRIES = "Entries";
    private static final String NBT_FROZEN = "Frozen";

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

    public void freeze(long now) {
        if (frozen) {
            return;
        }
        frozen = true;
        entries.replaceAll((key, entry) -> entry.closedAt(now));
    }

    /** Whether the log has been closed and takes no further changes. */
    public boolean isFrozen() {
        return frozen;
    }

    /** Every recorded reason, whether or not it still applies, in the order first seen. */
    public List<RecordedDelay> entries() {
        return List.copyOf(entries.values());
    }

    /** The recorded reasons themselves, in the order first seen. */
    public List<DelayInstance> instances() {
        return entries.values().stream().map(RecordedDelay::instance).toList();
    }

    /** The recorded reasons collapsed so that each cause appears only once. */
    public List<DelayInstance> reasons() {
        return DelayInstance.collapseByCause(instances());
    }

    /** Only the reasons that still apply. */
    public List<RecordedDelay> active() {
        return entries.values().stream().filter(RecordedDelay::isActive).toList();
    }

    /** Whether nothing has been recorded. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** How many reasons have been recorded. */
    public int size() {
        return entries.size();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_ENTRIES, NbtHelper.writeList(new ArrayList<>(entries.values()), RecordedDelay::toNbt));
        nbt.putBoolean(NBT_FROZEN, frozen);
        return nbt;
    }

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
