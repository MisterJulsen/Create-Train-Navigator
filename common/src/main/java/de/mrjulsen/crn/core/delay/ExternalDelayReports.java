package de.mrjulsen.crn.core.delay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.resources.ResourceLocation;

public final class ExternalDelayReports {

    public static final long NEVER_EXPIRES = -1;

    private record Report(DelayInstance instance, long expiresAt) {

        boolean isExpired(long now) {
            return expiresAt >= 0 && now >= expiresAt;
        }
    }

    private static final Map<UUID, Map<ResourceLocation, Report>> REPORTS = new ConcurrentHashMap<>();

    private ExternalDelayReports() {}

    public static boolean report(UUID trainId, ResourceLocation causeId, DelayArgument... args) {
        return report(trainId, causeId, NEVER_EXPIRES, DelayInstance.UNKNOWN_DELAY, args);
    }

    public static boolean report(UUID trainId, ResourceLocation causeId, long expiresInTicks, long estimatedDelayTicks, DelayArgument... args) {
        if (trainId == null || causeId == null) {
            return false;
        }
        DelaySeverity severity = DelayCauseRegistry.get(causeId).map(DelayCause::severity).orElse(null);
        if (severity == null) {
            return false;
        }

        long now = ModUtils.getTransformedWorldTime();
        Map<ResourceLocation, Report> byCause = REPORTS.computeIfAbsent(trainId, id -> new ConcurrentHashMap<>());
        Report previous = byCause.get(causeId);
        long since = previous == null ? now : previous.instance().since();

        DelayInstance instance = new DelayInstance(causeId, severity, since, estimatedDelayTicks,
            List.of(args), DelayOrigin.REPORTED);
        byCause.put(causeId, new Report(instance, expiresInTicks < 0 ? NEVER_EXPIRES : now + expiresInTicks));
        return true;
    }

    public static boolean withdraw(UUID trainId, ResourceLocation causeId) {
        Map<ResourceLocation, Report> byCause = REPORTS.get(trainId);
        if (byCause == null || byCause.remove(causeId) == null) {
            return false;
        }
        REPORTS.remove(trainId, Map.of());
        return true;
    }

    public static void withdrawAll(UUID trainId) {
        REPORTS.remove(trainId);
    }

    public static void clear() {
        REPORTS.clear();
    }

    public static List<DelayInstance> activeFor(UUID trainId, long now) {
        Map<ResourceLocation, Report> byCause = REPORTS.get(trainId);
        if (byCause == null || byCause.isEmpty()) {
            return List.of();
        }

        List<DelayInstance> active = new ArrayList<>(byCause.size());
        Collection<Map.Entry<ResourceLocation, Report>> entries = byCause.entrySet();
        entries.removeIf(entry -> entry.getValue().isExpired(now));
        for (Map.Entry<ResourceLocation, Report> entry : entries) {
            active.add(entry.getValue().instance());
        }
        return List.copyOf(active);
    }
}
