package de.mrjulsen.crn.core.delay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * One reason a train is delayed or disrupted, as it stands at a moment. Several instances of the same
 * cause can be merged so a train reports each reason once; see {@link #collapseByCause(Collection)}.
 * <p>
 * Times are in game ticks on the backend's time base.
 *
 * @param causeId             The id of the cause behind this delay.
 * @param severity            How the delay should be weighed against others.
 * @param since               When the delay began.
 * @param estimatedDelayTicks The time this cause is estimated to account for, or
 *                            {@link #UNKNOWN_DELAY} where no estimate is known.
 * @param args                Values filled into the cause's description, such as a blocking train's
 *                            name.
 * @param origin              Whether the delay was detected by the backend or reported from outside.
 */
public record DelayInstance(
    ResourceLocation causeId,
    DelaySeverity severity,
    long since,
    long estimatedDelayTicks,
    List<DelayArgument> args,
    DelayOrigin origin
) {

    /** The value of {@code estimatedDelayTicks} when no estimate is known. */
    public static final long UNKNOWN_DELAY = -1;

    private static final String NBT_CAUSE = "Cause";
    private static final String NBT_SEVERITY = "Severity";
    private static final String NBT_SINCE = "Since";
    private static final String NBT_ESTIMATED_DELAY = "EstimatedDelay";
    private static final String NBT_ARGS = "Args";
    private static final String NBT_ORIGIN = "Origin";

    public DelayInstance {
        args = args == null ? List.of() : List.copyOf(args);
        origin = origin == null ? DelayOrigin.DETECTED : origin;
    }

    public static DelayInstance of(ResourceLocation causeId, DelaySeverity severity, long since, List<DelayArgument> args) {
        return new DelayInstance(causeId, severity, since, UNKNOWN_DELAY, args, DelayOrigin.DETECTED);
    }

    public DelayInstance withSince(long since) {
        return new DelayInstance(causeId, severity, since, estimatedDelayTicks, args, origin);
    }

    public DelayInstance withEstimatedDelay(long ticks) {
        return new DelayInstance(causeId, severity, since, ticks, args, origin);
    }

    /** The translation key naming this cause, for showing it to a player. */
    public String translationKey() {
        return DelayCauseRegistry.get(causeId)
            .map(DelayCause::translationKey)
            .orElse("gui." + causeId.getNamespace() + ".delay_cause." + causeId.getPath());
    }

    /** Whether the description takes any values. */
    public boolean hasArgs() {
        return !args.isEmpty();
    }

    /** The values to fill into the description, in order. */
    public List<String> argValues() {
        return args.stream().map(DelayArgument::value).toList();
    }

    /** Whether an estimate of the time this cause accounts for is known. */
    public boolean hasEstimatedDelay() {
        return estimatedDelayTicks > UNKNOWN_DELAY;
    }

    /** How long the delay has lasted up to the given time, in ticks. */
    public long durationUntil(long now) {
        return Math.max(0, now - since);
    }

    /** The given delays merged so that each cause appears once, its arguments and estimates combined. */
    public static List<DelayInstance> collapseByCause(Collection<DelayInstance> instances) {
        Map<ResourceLocation, DelayInstance> byCause = new LinkedHashMap<>();
        for (DelayInstance instance : instances) {
            byCause.merge(instance.causeId(), instance, DelayInstance::mergedWith);
        }
        return List.copyOf(byCause.values());
    }

    private DelayInstance mergedWith(DelayInstance other) {
        List<DelayArgument> merged = new ArrayList<>(args);
        for (DelayArgument arg : other.args) {
            if (!merged.contains(arg)) {
                merged.add(arg);
            }
        }
        long estimated = hasEstimatedDelay() || other.hasEstimatedDelay()
            ? Math.max(0, estimatedDelayTicks) + Math.max(0, other.estimatedDelayTicks)
            : UNKNOWN_DELAY;
        return new DelayInstance(
            causeId,
            severity.compareTo(other.severity) >= 0 ? severity : other.severity,
            Math.min(since, other.since),
            estimated,
            merged,
            origin == other.origin ? origin : DelayOrigin.DETECTED
        );
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_CAUSE, causeId.toString());
        nbt.putByte(NBT_SEVERITY, (byte) severity.ordinal());
        nbt.putLong(NBT_SINCE, since);
        nbt.putLong(NBT_ESTIMATED_DELAY, estimatedDelayTicks);
        nbt.putString(NBT_ORIGIN, origin.name());
        ListTag argsTag = new ListTag();
        for (DelayArgument arg : args) {
            argsTag.add(arg.toNbt());
        }
        nbt.put(NBT_ARGS, argsTag);
        return nbt;
    }

    public static DelayInstance fromNbt(CompoundTag nbt) {
        DelaySeverity[] severities = DelaySeverity.values();
        int severityIndex = nbt.getByte(NBT_SEVERITY);

        ListTag argsTag = nbt.getList(NBT_ARGS, Tag.TAG_COMPOUND);
        List<DelayArgument> args = new ArrayList<>(argsTag.size());
        for (int i = 0; i < argsTag.size(); i++) {
            args.add(DelayArgument.fromNbt(argsTag.getCompound(i)));
        }

        DelayOrigin origin;
        try {
            origin = DelayOrigin.valueOf(nbt.getString(NBT_ORIGIN));
        } catch (IllegalArgumentException e) {
            origin = DelayOrigin.DETECTED;
        }

        return new DelayInstance(
            new ResourceLocation(nbt.getString(NBT_CAUSE)),
            severities[Math.floorMod(severityIndex, severities.length)],
            nbt.getLong(NBT_SINCE),
            nbt.contains(NBT_ESTIMATED_DELAY) ? nbt.getLong(NBT_ESTIMATED_DELAY) : UNKNOWN_DELAY,
            args,
            origin
        );
    }
}
