package de.mrjulsen.crn.core.delay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.registry.ModDelayCauses;
import net.minecraft.resources.ResourceLocation;

public final class DelayTracker {

    private static final Comparator<DelayInstance> REASON_ORDER =
        Comparator.comparingInt((DelayInstance d) -> d.severity().ordinal()).reversed()
            .thenComparingLong(DelayInstance::since);

    private final TrackedTrain train;
    private volatile List<DelayInstance> active = List.of();

    public DelayTracker(TrackedTrain train) {
        this.train = train;
    }

    public List<DelayInstance> getActive() {
        return active;
    }

    public void clear() {
        this.active = List.of();
    }

    public void restore(List<DelayInstance> reasons) {
        this.active = List.copyOf(reasons);
    }

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

    private static Object identityOf(DelayInstance instance) {
        return identityOf(instance.causeId(), instance.args());
    }

    private static Object identityOf(ResourceLocation causeId, List<DelayArgument> args) {
        return List.of(causeId, args);
    }

    private boolean isDelayed() {
        return train.isDelayed() || train.getDelayOffset() > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

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

    public record DisruptionOutcome(boolean visible, boolean discardWhenExpired) {}

    public DisruptionOutcome evaluateDisruption(long now) {
        int fallback = ModCommonConfig.DISRUPTION_DISPLAY_DURATION.get();
        boolean deliberate = active.stream().anyMatch(x -> x.severity() == DelaySeverity.IMPORTANT && handlingOf(x) == DisruptionHandling.DELIBERATE);
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
