package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class NbtHelper {

    private NbtHelper() {}

    public static <T> ListTag writeList(List<T> values, Function<T, CompoundTag> writer) {
        ListTag list = new ListTag();
        for (T value : values) {
            list.add(writer.apply(value));
        }
        return list;
    }

    public static <T> List<T> readList(CompoundTag nbt, String key, Function<CompoundTag, T> reader) {
        if (nbt == null || !nbt.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        List<T> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            values.add(reader.apply(list.getCompound(i)));
        }
        return values;
    }

    public static <T extends Enum<T>> T readEnum(String name, Class<T> type, T fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static ListTag writeStrings(Collection<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(net.minecraft.nbt.StringTag.valueOf(value));
        }
        return list;
    }

    public static List<String> readStrings(CompoundTag nbt, String key) {
        if (nbt == null || !nbt.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = nbt.getList(key, Tag.TAG_STRING);
        List<String> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getString(i));
        }
        return values;
    }

    public static ListTag writeUuids(Collection<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            list.add(net.minecraft.nbt.NbtUtils.createUUID(id));
        }
        return list;
    }

    public static Set<UUID> readUuids(CompoundTag nbt, String key) {
        if (nbt == null || !nbt.contains(key, Tag.TAG_LIST)) {
            return Set.of();
        }
        ListTag list = nbt.getList(key, Tag.TAG_INT_ARRAY);
        Set<UUID> ids = new LinkedHashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ids.add(net.minecraft.nbt.NbtUtils.loadUUID(list.get(i)));
        }
        return ids;
    }

    public static UUID readNullableUUID(CompoundTag nbt, String key) {
        return nbt != null && nbt.hasUUID(key) ? nbt.getUUID(key) : null;
    }

    public static void putNullableUUID(CompoundTag nbt, String key, UUID value) {
        if (value != null) {
            nbt.putUUID(key, value);
        }
    }
}
