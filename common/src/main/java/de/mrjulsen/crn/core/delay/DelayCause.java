package de.mrjulsen.crn.core.delay;

import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.config.ModServerConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * A reason a train may be delayed or disrupted. Extend this and register it with
 * {@link DelayCauseRegistry} to have the backend consult it whenever it examines a train. A cause
 * decides for itself, from the {@link DelayContext} it is handed, whether it currently applies, and
 * reports an occurrence where it does.
 */
public abstract class DelayCause {

    private ResourceLocation id;

    /** How severe delays from this cause are. */
    public abstract DelaySeverity severity();

    /** Examines the train through the given context and reports the occurrences it finds, or none. */
    public abstract Collection<DelayInstance> detect(DelayContext context);

    /** The translation key naming this cause, for showing it to a player. */
    public String translationKey() {
        return "gui." + id.getNamespace() + ".delay_cause." + id.getPath();
    }

    /** The id this cause was registered under. */
    public final ResourceLocation id() {
        return id;
    }

    /** How long to keep showing this cause after the train has gone out of service, in ticks. */
    public int displayDurationWhileOutOfService() {
        return ModServerConfig.DISRUPTION_DISPLAY_DURATION.get();
    }

    /** A retention duration meaning the cause is kept for as long as it applies, without limit. */
    public static final int RETENTION_UNLIMITED = -1;

    /** How a disruption from this cause should be treated; a fault of the train by default. */
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.FAULT;
    }

    /** Reports a single occurrence of this cause, with the given values filled into its description. */
    protected final Collection<DelayInstance> present(DelayContext context, DelayArgument... args) {
        return List.of(occurrence(context, args));
    }

    /** The same as {@link #present(DelayContext, DelayArgument...)}, with an estimate of the time lost, in ticks. */
    protected final Collection<DelayInstance> present(DelayContext context, long estimatedDelayTicks, DelayArgument... args) {
        return List.of(occurrence(context, args).withEstimatedDelay(estimatedDelayTicks));
    }

    /** Reports that this cause does not currently apply. */
    protected final Collection<DelayInstance> absent() {
        return List.of();
    }

    /** A single occurrence of this cause as of now, with the given description values. */
    protected final DelayInstance occurrence(DelayContext context, DelayArgument... args) {
        return DelayInstance.of(id, severity(), context.now(), List.of(args));
    }

    final void assignId(ResourceLocation id) {
        this.id = id;
    }
}
