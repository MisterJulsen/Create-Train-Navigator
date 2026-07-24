package de.mrjulsen.crn.api.core;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * One speed restriction on the track ahead of a train. A segment applies from where it starts until
 * the next segment starts, so a stretch of track is described by a series of them.
 *
 * @param startDistance  Where the restriction begins, in blocks ahead of the train. Never negative.
 * @param speedLimit     The highest permitted speed from there on, in blocks per tick, using the
 *                       same unit as Create's train speeds. Never negative; zero means the train
 *                       may not proceed.
 * @param kind           What sort of restriction this is.
 * @param source         The provider that supplied the segment. Filled in by the backend, so a
 *                       provider may leave it {@code null}.
 * @param descriptionKey A translation key naming the reason, or {@code null} for none.
 */
public record SpeedLimitSegment(
    double startDistance,
    double speedLimit,
    SpeedLimitKind kind,
    ResourceLocation source,
    String descriptionKey
) {

    public SpeedLimitSegment {
        startDistance = Math.max(0, startDistance);
        speedLimit = Math.max(0, speedLimit);
        kind = kind == null ? SpeedLimitKind.OTHER : kind;
    }

    public static SpeedLimitSegment of(double startDistance, double speedLimit) {
        return new SpeedLimitSegment(startDistance, speedLimit, SpeedLimitKind.PERMANENT, null, null);
    }

    public static SpeedLimitSegment of(double startDistance, double speedLimit, SpeedLimitKind kind) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, null, null);
    }

    /** The same segment with a translation key naming the reason for the restriction. */
    public SpeedLimitSegment describedBy(String descriptionKey) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, source, descriptionKey);
    }

    /** The same segment attributed to a provider. Applied by the backend when it collects them. */
    public SpeedLimitSegment attributedTo(ResourceLocation source) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, source, descriptionKey);
    }

    public boolean hasDescription() {
        return descriptionKey != null && !descriptionKey.isBlank();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble(NBT_START_DISTANCE, startDistance);
        nbt.putDouble(NBT_SPEED_LIMIT, speedLimit);
        nbt.putString(NBT_KIND, kind.name());
        if (source != null) {
            nbt.putString(NBT_SOURCE, source.toString());
        }
        if (descriptionKey != null) {
            nbt.putString(NBT_DESCRIPTION_KEY, descriptionKey);
        }
        return nbt;
    }

    public static SpeedLimitSegment fromNbt(CompoundTag nbt) {
        return new SpeedLimitSegment(
            nbt.getDouble(NBT_START_DISTANCE),
            nbt.getDouble(NBT_SPEED_LIMIT),
            NbtHelper.readEnum(nbt.getString(NBT_KIND), SpeedLimitKind.class, SpeedLimitKind.OTHER),
            nbt.contains(NBT_SOURCE) ? new ResourceLocation(nbt.getString(NBT_SOURCE)) : null,
            nbt.contains(NBT_DESCRIPTION_KEY) ? nbt.getString(NBT_DESCRIPTION_KEY) : null
        );
    }

    private static final String NBT_START_DISTANCE = "StartDistance";
    private static final String NBT_SPEED_LIMIT = "SpeedLimit";
    private static final String NBT_KIND = "Kind";
    private static final String NBT_SOURCE = "Source";
    private static final String NBT_DESCRIPTION_KEY = "DescriptionKey";
}
