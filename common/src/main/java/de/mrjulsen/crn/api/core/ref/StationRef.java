package de.mrjulsen.crn.api.core.ref;

import java.util.UUID;

import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.data.settings.StationTag.StationInfo;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.nbt.CompoundTag;

/**
 * A station as referred to from elsewhere in the API: its own name, and the station tag it belongs
 * to if it has one.
 * <p>
 * A station tag groups several stations that travellers regard as one place. Where a station is
 * shown to a player, {@link #displayName()} should be preferred over {@link #name()}, because it
 * yields the tag name when one exists.
 *
 * @param name    The station's own name, as configured in the world.
 * @param tagName The name of the tag this station belongs to, or empty if it has none.
 * @param tagId   The id of that tag, or {@code null}.
 * @param info    Per-station details held by the tag, such as the platform.
 */
public record StationRef(String name, String tagName, UUID tagId, StationInfo info) {

    /** Stands for "no station". Its name is empty and {@link #isKnown()} is false. */
    public static final StationRef NONE = new StationRef("", "", null, StationInfo.empty());

    private static final String NBT_NAME = "Name";
    private static final String NBT_TAG_NAME = "TagName";
    private static final String NBT_TAG_ID = "TagId";
    private static final String NBT_INFO = "Info";

    public StationRef {
        name = name == null ? "" : name;
        tagName = tagName == null ? "" : tagName;
        info = info == null ? StationInfo.empty() : info;
    }

    public static StationRef of(String stationName) {
        if (stationName == null || stationName.isBlank()) {
            return NONE;
        }
        return of(stationName, StationLookup.findTag(stationName));
    }

    public static StationRef of(String stationName, StationTag tag) {
        if (stationName == null || stationName.isBlank()) {
            return NONE;
        }
        if (tag == null) {
            return new StationRef(stationName, "", null, StationInfo.empty());
        }
        return new StationRef(
            stationName,
            tag.getTagName() == null ? "" : tag.getTagName().get(),
            tag.getId(),
            tag.getInfoForStation(stationName)
        );
    }

    public boolean hasTag() {
        return !tagName.isBlank();
    }

    /** The name to show a player: the tag name if the station has one, otherwise its own name. */
    public String displayName() {
        return hasTag() ? tagName : name;
    }

    /** The platform at this station, or an empty string if none is configured. */
    public String platform() {
        return info.platform();
    }

    public boolean hasPlatform() {
        return info.isPlatformKnown();
    }

    /** Whether this refers to a station at all, as opposed to being {@link #NONE}. */
    public boolean isKnown() {
        return !name.isBlank();
    }

    /**
     * Whether this station is meant by the given name, which may also be a station filter using the
     * same wildcard syntax as a train schedule.
     */
    public boolean matches(String stationNameOrFilter) {
        return isKnown() && TrainUtils.stationMatches(name, stationNameOrFilter);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_NAME, name);
        nbt.putString(NBT_TAG_NAME, tagName);
        if (tagId != null) {
            nbt.putUUID(NBT_TAG_ID, tagId);
        }
        nbt.put(NBT_INFO, info.toNbt());
        return nbt;
    }

    public static StationRef fromNbt(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return NONE;
        }
        return new StationRef(
            nbt.getString(NBT_NAME),
            nbt.getString(NBT_TAG_NAME),
            nbt.hasUUID(NBT_TAG_ID) ? nbt.getUUID(NBT_TAG_ID) : null,
            StationInfo.fromNbt(nbt.getCompound(NBT_INFO))
        );
    }

    @Override
    public String toString() {
        return name;
    }
}
