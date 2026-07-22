package de.mrjulsen.crn.backend.api;

import java.util.UUID;

import de.mrjulsen.crn.backend.util.StationLookup;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.train.TrainUtils;
import net.minecraft.nbt.CompoundTag;

/**
 * A station as this API refers to it: its name in the track network together with everything its
 * station tag knows about it, copied in rather than looked up.
 * <p>
 * Everywhere a station appears - a stop, a board row, a section's origin or terminus - it appears as
 * this record, which makes the tag name and the platform available without a second query and
 * without holding on to the mutable {@link StationTag} behind it. That is what lets a station
 * survive being sent to a client or written to JSON: it carries its own answers.
 *
 * @param name    The station's name in the track network. Empty if no station is known.
 * @param tagName The name of the station tag this station belongs to, or empty if it carries none.
 *                A station in several tags reports the first one that contains it.
 * @param tagId   The id of that tag, or {@code null} if the station carries none.
 * @param info    What the tag knows about this station, never {@code null}.
 */
public record StationRef(String name, String tagName, UUID tagId, StationInfo info) {

    /** No station, e.g. for a stop whose station could not be resolved. */
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

    /** The given station with its tag resolved from the global settings. */
    public static StationRef of(String stationName) {
        if (stationName == null || stationName.isBlank()) {
            return NONE;
        }
        return of(stationName, StationLookup.findTag(stationName));
    }

    /** The given station with an already known tag, which may be {@code null}. */
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

    /** The station's name, under the name the display data uses for it. */
    public String stationName() {
        return name;
    }

    /** Whether this station belongs to a station tag at all. */
    public boolean hasTag() {
        return !tagName.isBlank();
    }

    /** The name of this station's tag, or the station's own name if it carries none. */
    public String displayName() {
        return hasTag() ? tagName : name;
    }

    /** The platform assigned to this station by its tag, or empty if none is known. */
    public String platform() {
        return info.platform();
    }

    /** Whether a platform is known for this station. */
    public boolean hasPlatform() {
        return info.isPlatformKnown();
    }

    /** Whether this refers to a station at all. */
    public boolean isKnown() {
        return !name.isBlank();
    }

    /** Whether this station matches the given name or filter, which may contain {@code *}. */
    public boolean matches(String stationNameOrFilter) {
        return isKnown() && TrainUtils.stationMatches(name, stationNameOrFilter);
    }

    /** Serializes this station. */
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

    /** Deserializes a station written by {@link #toNbt()}. */
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
