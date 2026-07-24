package de.mrjulsen.crn.core.delay;

import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.resources.ResourceLocation;

public abstract class DelayCause {

    private ResourceLocation id;

    public abstract DelaySeverity severity();

    public abstract Collection<DelayInstance> detect(DelayContext context);

    public String translationKey() {
        return "gui." + id.getNamespace() + ".delay_cause." + id.getPath();
    }

    public final ResourceLocation id() {
        return id;
    }

    public int displayDurationWhileOutOfService() {
        return ModCommonConfig.DISRUPTION_DISPLAY_DURATION.get();
    }

    public static final int RETENTION_UNLIMITED = -1;

    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.FAULT;
    }

    protected final Collection<DelayInstance> present(DelayContext context, DelayArgument... args) {
        return List.of(occurrence(context, args));
    }

    protected final Collection<DelayInstance> present(DelayContext context, long estimatedDelayTicks, DelayArgument... args) {
        return List.of(occurrence(context, args).withEstimatedDelay(estimatedDelayTicks));
    }

    protected final Collection<DelayInstance> absent() {
        return List.of();
    }

    protected final DelayInstance occurrence(DelayContext context, DelayArgument... args) {
        return DelayInstance.of(id, severity(), context.now(), List.of(args));
    }

    final void assignId(ResourceLocation id) {
        this.id = id;
    }
}
