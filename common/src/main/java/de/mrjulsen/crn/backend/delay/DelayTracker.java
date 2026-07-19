package de.mrjulsen.crn.backend.delay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import de.mrjulsen.crn.backend.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.registry.ModDelayCauses;
import net.minecraft.resources.ResourceLocation;

/**
 * Maintains the status reasons of one train: which {@link DelayCause}s currently apply, since when,
 * and what that means for a train that is out of service.
 * <p>
 * The reason list is not recomputed from scratch each pass, it accumulates. Once a cause has made
 * the train late it stays until the train is back on time, so the original reason for a standing
 * delay is not overwritten by a later symptom. Further causes are added on top as they occur, and
 * the generic fallback only shows while nothing more specific explains the delay. Each reason keeps
 * its first-seen time, so it reports when it started rather than when it was last polled. The
 * accumulation is cleared on a section change, where the remaining delay is carried over instead.
 *
 * <h2>Threading</h2>
 * Owned by its {@link TrackedTrain}, which calls {@link #refresh(long)} inside its update lock. The
 * list is published through a volatile field and always replaced as a whole, so readers on other
 * threads see either the previous or the new one, never a partial list.
 */
public final class DelayTracker {

    /** IMPORTANT reasons first, then by first-seen time so the original cause precedes later ones. */
    private static final Comparator<DelayInstance> REASON_ORDER =
        Comparator.comparingInt((DelayInstance d) -> d.severity().ordinal()).reversed()
            .thenComparingLong(DelayInstance::since);

    private final TrackedTrain train;
    private volatile List<DelayInstance> active = List.of();

    public DelayTracker(TrackedTrain train) {
        this.train = train;
    }

    /** The reasons currently applying to the train. Immutable. */
    public List<DelayInstance> getActive() {
        return active;
    }

    /** Drops all accumulated reasons, e.g. on a section change or when the train restarts. */
    public void clear() {
        this.active = List.of();
    }

    /** Restores persisted reasons (only stored for trains that are out of service). */
    public void restore(List<DelayInstance> reasons) {
        this.active = List.copyOf(reasons);
    }

    /**
     * Runs every registered cause against the current train state and updates the reason list.
     * While the train is out of service only the operational reasons are considered; the delay
     * causes are not polled at all, since their answers would be discarded.
     */
    public void refresh(long now) {
        boolean operationalOnly = train.isCancelled();

        DelayContext context = new DelayContext(train, now);
        List<DelayInstance> detected = new ArrayList<>();
        for (DelayCause cause : DelayCauseRegistry.all()) {
            if (operationalOnly && cause.severity() != DelaySeverity.IMPORTANT) {
                continue;
            }
            detected.addAll(cause.detect(context));
        }

        for (DelayInstance reported : ExternalDelayReports.activeFor(train.getTrainId(), now)) {
            if (operationalOnly && reported.severity() != DelaySeverity.IMPORTANT) {
                continue;
            }
            detected.removeIf(x -> x.causeId().equals(reported.causeId()));
            detected.add(reported);
        }

        List<DelayInstance> previous = this.active;
        this.active = operationalOnly ? buildOperational(detected) : buildFull(detected);

        if (!sameReasons(previous, this.active)) {
            RailwayBackendEvents.fireDelaysChanged(train);
        }
    }

    /** Whether two reason lists describe the same situation. */
    private static boolean sameReasons(List<DelayInstance> a, List<DelayInstance> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!identityOf(a.get(i)).equals(identityOf(b.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private List<DelayInstance> buildOperational(List<DelayInstance> detected) {
        List<DelayInstance> operational = new ArrayList<>(detected.size());
        for (DelayInstance instance : detected) {
            operational.add(keepFirstSeen(instance));
        }
        operational.sort(REASON_ORDER);
        return List.copyOf(operational);
    }

    private List<DelayInstance> buildFull(List<DelayInstance> detected) {
        List<DelayInstance> reasons = new ArrayList<>();

        for (DelayInstance instance : detected) {
            if (instance.severity() != DelaySeverity.DELAY) {
                reasons.add(keepFirstSeen(instance));
            }
        }

        if (isDelayed()) {
            LinkedHashMap<Object, DelayInstance> accumulated = new LinkedHashMap<>();
            for (DelayInstance previous : active) {
                if (previous.severity() == DelaySeverity.DELAY) {
                    accumulated.put(identityOf(previous), previous);
                }
            }
            for (DelayInstance instance : detected) {
                if (instance.severity() == DelaySeverity.DELAY) {
                    accumulated.putIfAbsent(identityOf(instance), instance);
                }
            }
            if (accumulated.size() > 1) {
                accumulated.keySet().removeIf(key -> key.equals(identityOf(ModDelayCauses.UNKNOWN_DELAY.id(), List.of())));
            }
            reasons.addAll(accumulated.values());
        }

        reasons.sort(REASON_ORDER);
        return List.copyOf(reasons);
    }

    /**
     * What makes two occurrences "the same reason" across passes. A cause may report several
     * occurrences at once (one per blocking train, say), so the cause id alone is not enough - the
     * arguments are what distinguish them.
     */
    private static Object identityOf(DelayInstance instance) {
        return identityOf(instance.causeId(), instance.args());
    }

    private static Object identityOf(ResourceLocation causeId, List<DelayArgument> args) {
        return List.of(causeId, args);
    }

    /** Whether the train counts as delayed, including deviation carried over. */
    private boolean isDelayed() {
        return train.isDelayed() || train.getDelayOffset() > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /**
     * Carries the first-seen time over from the previous pass; without it a cancelled train would
     * claim to have been cancelled just now every few seconds. A reported reason is left alone,
     * since it tracks its own first-seen time across re-reports.
     */
    private DelayInstance keepFirstSeen(DelayInstance instance) {
        if (instance.origin() == DelayOrigin.REPORTED) {
            return instance;
        }
        for (DelayInstance previous : active) {
            if (identityOf(previous).equals(identityOf(instance))) {
                return instance.withSince(previous.since());
            }
        }
        return instance;
    }

    /**
     * What is to become of a train that is out of service: whether it is still worth showing, and
     * whether its data may be thrown away once it is not.
     */
    public record DisruptionOutcome(boolean visible, boolean discardWhenExpired) {}

    /**
     * Decides the fate of the current disruption from the reasons that caused it. Each reason keeps
     * the train visible for its own duration, counted from when it was first detected, so the
     * longest one wins.
     * <p>
     * A {@link DisruptionHandling#DELIBERATE} reason overrules any fault applying alongside it, and
     * only the deliberate ones are then consulted: taking a faulty train out of service on purpose
     * outranks the fault's own retention. That same distinction decides whether the train is
     * forgotten afterwards.
     * <p>
     * A reason whose cause is no longer registered falls back to the default duration and counts as
     * a fault. A disruption without any operational reason keeps the train visible, since there is
     * nothing to measure the elapsed time from.
     */
    public DisruptionOutcome evaluateDisruption(long now) {
        int fallback = ModCommonConfig.DISRUPTION_DISPLAY_DURATION.get();
        boolean deliberate = active.stream().anyMatch(x -> x.severity() == DelaySeverity.IMPORTANT
            && handlingOf(x) == DisruptionHandling.DELIBERATE);
        boolean anyReason = false;

        for (DelayInstance instance : active) {
            if (instance.severity() != DelaySeverity.IMPORTANT) {
                continue;
            }
            if (deliberate && handlingOf(instance) != DisruptionHandling.DELIBERATE) {
                continue;
            }
            anyReason = true;
            int duration = DelayCauseRegistry.get(instance.causeId())
                .map(DelayCause::displayDurationWhileOutOfService)
                .orElse(fallback);
            if (duration < 0 || now - instance.since() < duration) {
                return new DisruptionOutcome(true, deliberate);
            }
        }
        return new DisruptionOutcome(!anyReason, deliberate && anyReason);
    }

    private DisruptionHandling handlingOf(DelayInstance instance) {
        return DelayCauseRegistry.get(instance.causeId())
            .map(DelayCause::disruptionHandling)
            .orElse(DisruptionHandling.FAULT);
    }
}
