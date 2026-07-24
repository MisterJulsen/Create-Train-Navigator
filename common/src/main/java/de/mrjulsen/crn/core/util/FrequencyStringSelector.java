package de.mrjulsen.crn.core.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class FrequencyStringSelector {

    private static final String NBT_WINDOW = "Window";

    private final int windowSize;
    private final Deque<String> window = new ArrayDeque<>();
    private final Map<String, Integer> frequency = new HashMap<>();
    private volatile String primary = null;

    public FrequencyStringSelector(int windowSize) {
        this.windowSize = Math.max(1, windowSize);
    }

    public synchronized void add(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        window.addLast(value);
        frequency.merge(value, 1, Integer::sum);
        while (window.size() > windowSize) {
            String removed = window.pollFirst();
            frequency.computeIfPresent(removed, (k, v) -> v <= 1 ? null : v - 1);
        }
        recomputePrimary();
    }

    private void recomputePrimary() {
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        if (best == null || primary == null) {
            primary = best;
        } else if (!best.equals(primary) && bestCount > frequency.getOrDefault(primary, 0)) {
            primary = best;
        }
    }

    public String getPrimary() {
        return primary;
    }

    public synchronized void reset() {
        window.clear();
        frequency.clear();
        primary = null;
    }

    public synchronized CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (String value : window) {
            list.add(StringTag.valueOf(value));
        }
        nbt.put(NBT_WINDOW, list);
        return nbt;
    }

    public synchronized void loadNbt(CompoundTag nbt) {
        reset();
        ListTag list = nbt.getList(NBT_WINDOW, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            add(list.getString(i));
        }
    }
}
