package de.mrjulsen.crn.backend.delay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.resources.ResourceLocation;

/**
 * Lets systems outside the backend attach a status reason to a train, instead of waiting to be
 * polled by a {@link DelayCause}.
 * <p>
 * A {@link DelayCause} answers the question "does this apply right now?" and is asked once per
 * backend update. That fits anything derivable from the train's live state, but not a reason that
 * originates as an <em>event</em> and cannot be re-derived afterwards - a dispatcher taking a line
 * out of service, a signalling failure announced by another system, a command or web request. Those
 * are reported here and stay attached until they expire or are withdrawn.
 * <p>
 * A reported reason must still refer to a {@linkplain DelayCauseRegistry registered} cause, so it
 * carries a translation and a severity like any other. Reports are deliberately <b>not</b>
 * persisted: they describe a live operational situation, and whoever raised one is responsible for
 * raising it again after a restart if it still applies.
 *
 * <h2>Threading</h2>
 * Safe to call from any thread.
 */
public final class ExternalDelayReports {

    /** Value for {@code expiresInTicks} meaning the report applies until it is withdrawn. */
    public static final long NEVER_EXPIRES = -1;

    private record Report(DelayInstance instance, long expiresAt) {

        boolean isExpired(long now) {
            return expiresAt >= 0 && now >= expiresAt;
        }
    }

    private static final Map<UUID, Map<ResourceLocation, Report>> REPORTS = new ConcurrentHashMap<>();

    private ExternalDelayReports() {}

    /**
     * Attaches a reason to a train until it is withdrawn.
     *
     * @param trainId The train the reason applies to.
     * @param causeId The registered cause describing the reason.
     * @param args    Arguments for the cause's message.
     * @return Whether the report was accepted. Rejected if the cause is not registered.
     * @see #report(UUID, ResourceLocation, long, long, DelayArgument...)
     */
    public static boolean report(UUID trainId, ResourceLocation causeId, DelayArgument... args) {
        return report(trainId, causeId, NEVER_EXPIRES, DelayInstance.UNKNOWN_DELAY, args);
    }

    /**
     * Attaches a reason to a train, replacing any earlier report of the same cause for that train.
     * <p>
     * Reporting the same cause again refreshes its expiry but keeps its original first-seen time, so
     * a reason that is re-reported periodically to keep it alive still shows how long it has really
     * been applying.
     *
     * @param trainId             The train the reason applies to.
     * @param causeId             The registered cause describing the reason.
     * @param expiresInTicks      How long the report stays attached, or {@link #NEVER_EXPIRES} to
     *                            keep it until it is withdrawn.
     * @param estimatedDelayTicks How much delay this reason accounts for, or
     *                            {@link DelayInstance#UNKNOWN_DELAY} if unknown. Informational only.
     * @param args                Arguments for the cause's message.
     * @return Whether the report was accepted. Rejected if the cause is not registered, since the
     *         reason could then be neither translated nor prioritized.
     */
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

    /**
     * Removes a previously reported reason.
     *
     * @return Whether a report of that cause was attached to the train.
     */
    public static boolean withdraw(UUID trainId, ResourceLocation causeId) {
        Map<ResourceLocation, Report> byCause = REPORTS.get(trainId);
        if (byCause == null || byCause.remove(causeId) == null) {
            return false;
        }
        REPORTS.remove(trainId, Map.of());
        return true;
    }

    /** Removes all reported reasons of one train. */
    public static void withdrawAll(UUID trainId) {
        REPORTS.remove(trainId);
    }

    /** Removes every reported reason of every train. */
    public static void clear() {
        REPORTS.clear();
    }

    /** The reasons currently reported for a train, excluding any that have expired. */
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
