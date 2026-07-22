package de.mrjulsen.crn.backend.delay;

import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * A registered type of status reason shown to travellers: either a delay reason or an operational
 * one that takes the train out of service.
 * <p>
 * To add a reason, subclass this, implement {@link #severity()} and {@link #detect(DelayContext)},
 * and register the instance once via
 * {@link DelayCauseRegistry#register(String, String, DelayCause)}. Detection is polled for every
 * tracked train on each backend update. The display name comes from a translation key derived from
 * the registration id, so only a language entry is needed - no rendering or networking code.
 */
public abstract class DelayCause {

    private ResourceLocation id;

    /** The severity this reason is sorted and coloured by. */
    public abstract DelaySeverity severity();

    /**
     * Decides whether, and in how many ways, this reason currently applies to the given train.
     * <p>
     * Most causes describe a single situation and return {@link #present(DelayContext,
     * DelayArgument...)} or {@link #absent()}. A cause may however report several occurrences at
     * once - one per blocking train, per affected section, and so on - by returning them all; each
     * is tracked and displayed separately.
     *
     * @return Every occurrence that currently applies. Empty if the reason does not apply.
     */
    public abstract Collection<DelayInstance> detect(DelayContext context);

    /**
     * The translation key of this cause's display name. Defaults to
     * {@code gui.<namespace>.delay_cause.<path>} of the registration id.
     * <p>
     * The name states what kind of reason this is and nothing more - no train names, no stations.
     * Anything specific to the occurrence belongs in {@link #detailsTranslationKey()}, so a view can
     * show the reason on its own the way a departure board does.
     */
    public String translationKey() {
        return "gui." + id.getNamespace() + ".delay_cause." + id.getPath();
    }

    /** The registration id of this cause. */
    public final ResourceLocation id() {
        return id;
    }

    /**
     * How long a train out of service because of this reason keeps being shown as cancelled, in
     * ticks, measured from when the reason was first detected. Afterwards the train drops off the
     * displays, keeping its data so it returns immediately once it runs again.
     * <p>
     * Only consulted for {@link DelaySeverity#IMPORTANT} reasons. When several apply the longest
     * wins, except that a {@link DisruptionHandling#DELIBERATE} reason overrules any fault.
     *
     * @return The duration in ticks, {@link #RETENTION_UNLIMITED} to keep the train visible until
     *         it runs again, or {@code 0} to never show it as cancelled at all.
     */
    public int displayDurationWhileOutOfService() {
        return ModCommonConfig.DISRUPTION_DISPLAY_DURATION.get();
    }

    /** Return this from {@link #displayDurationWhileOutOfService()} to show it until it runs again. */
    public static final int RETENTION_UNLIMITED = -1;

    /**
     * Whether a train out of service because of this reason was parked deliberately or hit by a
     * fault. Decides what happens once {@link #displayDurationWhileOutOfService()} has elapsed, and
     * a deliberate reason takes precedence over any fault applying at the same time.
     * <p>
     * Defaults to {@link DisruptionHandling#FAULT}, which never discards anything.
     */
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.FAULT;
    }

    /** Reports a single occurrence of this cause with the given message arguments. */
    protected final Collection<DelayInstance> present(DelayContext context, DelayArgument... args) {
        return List.of(occurrence(context, args));
    }

    /**
     * Reports a single occurrence that also states how much of the train's delay it accounts for.
     * The value is informational only; the backend always measures the actual delay itself.
     */
    protected final Collection<DelayInstance> present(DelayContext context, long estimatedDelayTicks, DelayArgument... args) {
        return List.of(occurrence(context, args).withEstimatedDelay(estimatedDelayTicks));
    }

    /** Reports that this cause does not apply. */
    protected final Collection<DelayInstance> absent() {
        return List.of();
    }

    /**
     * Builds a single occurrence without reporting it, for a cause that assembles several at once
     * and returns them as one collection.
     */
    protected final DelayInstance occurrence(DelayContext context, DelayArgument... args) {
        return DelayInstance.of(id, severity(), context.now(), List.of(args));
    }

    /** Assigned once by the registry; not part of the public API. */
    final void assignId(ResourceLocation id) {
        this.id = id;
    }
}
