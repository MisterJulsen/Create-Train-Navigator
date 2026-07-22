package de.mrjulsen.crn.backend.delay;

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
 * A concrete occurrence of a {@link DelayCause} at a train: which cause applies, since when, how
 * much of the delay it accounts for, and the metadata needed to render its message.
 * <p>
 * The metadata is kept as {@linkplain DelayArgument tagged arguments} that fill the cause's
 * translation on the client. The server never resolves the translation itself, so language
 * selection stays a client concern.
 *
 * @param causeId             The id of the cause this occurrence belongs to.
 * @param severity            The severity of this occurrence. Copied in rather than read from the
 *                            cause, so the record stays self-contained even if the cause is not
 *                            registered on the receiving side, and so one cause can report
 *                            occurrences of differing severity.
 * @param since               The transformed game time at which this occurrence was first detected.
 * @param estimatedDelayTicks How much of the train's delay this occurrence accounts for, in ticks,
 *                            or {@link #UNKNOWN_DELAY} if the cause cannot quantify it. Purely
 *                            informational: the backend measures the actual delay itself and never
 *                            derives it from these values.
 * @param args                Arguments for the cause's message. May be empty, never {@code null}.
 * @param origin              Whether this occurrence was detected by the backend or pushed in.
 */
public record DelayInstance(
    ResourceLocation causeId,
    DelaySeverity severity,
    long since,
    long estimatedDelayTicks,
    List<DelayArgument> args,
    DelayOrigin origin
) {

    /** Value of {@link #estimatedDelayTicks()} when the cause cannot quantify its contribution. */
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

    /** A detected occurrence with an unquantified delay contribution. */
    public static DelayInstance of(ResourceLocation causeId, DelaySeverity severity, long since, List<DelayArgument> args) {
        return new DelayInstance(causeId, severity, since, UNKNOWN_DELAY, args, DelayOrigin.DETECTED);
    }

    /** A copy of this occurrence with a different first-seen time. */
    public DelayInstance withSince(long since) {
        return new DelayInstance(causeId, severity, since, estimatedDelayTicks, args, origin);
    }

    /** A copy of this occurrence with a quantified delay contribution. */
    public DelayInstance withEstimatedDelay(long ticks) {
        return new DelayInstance(causeId, severity, since, ticks, args, origin);
    }

    /**
     * The translation key naming this occurrence's kind of reason, resolved through the registry.
     * This is what a view shows: the reason and nothing else, without any of the particulars.
     */
    public String translationKey() {
        return DelayCauseRegistry.get(causeId)
            .map(DelayCause::translationKey)
            .orElse("gui." + causeId.getNamespace() + ".delay_cause." + causeId.getPath());
    }

    /** Whether this occurrence carries any message arguments. */
    public boolean hasArgs() {
        return !args.isEmpty();
    }

    /** The printable values of all arguments, for consumers that do not care about their types. */
    public List<String> argValues() {
        return args.stream().map(DelayArgument::value).toList();
    }

    /** Whether the cause was able to quantify how much delay it accounts for. */
    public boolean hasEstimatedDelay() {
        return estimatedDelayTicks > UNKNOWN_DELAY;
    }

    /** How long this occurrence has been applying at the given time, in ticks. */
    public long durationUntil(long now) {
        return Math.max(0, now - since);
    }

    /**
     * Collapses occurrences of the same cause into one, for a view that shows a reason by name.
     * <p>
     * Two occurrences of one cause are two separate events - a different train in the way, or the
     * same situation a second time - and are tracked separately so each keeps its own first-seen
     * time. Named without their particulars they read as the same line twice, which tells a traveller
     * nothing, so what is shown is one line per reason with the particulars of all of them gathered
     * behind it.
     *
     * @return One occurrence per cause, in the order the causes first appeared.
     */
    public static List<DelayInstance> collapseByCause(Collection<DelayInstance> instances) {
        Map<ResourceLocation, DelayInstance> byCause = new LinkedHashMap<>();
        for (DelayInstance instance : instances) {
            byCause.merge(instance.causeId(), instance, DelayInstance::mergedWith);
        }
        return List.copyOf(byCause.values());
    }

    /**
     * This occurrence and another of the same cause as one: it began when the earlier of them did,
     * weighs as heavily as the more severe of them, and carries the arguments of both. Their
     * quantified shares are added up, since each accounts for a part of the same delay.
     */
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

    /** Serializes this occurrence. */
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

    /** Deserializes an occurrence written by {@link #toNbt()}. */
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
