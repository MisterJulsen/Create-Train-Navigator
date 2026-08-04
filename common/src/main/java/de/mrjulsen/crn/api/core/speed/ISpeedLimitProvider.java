package de.mrjulsen.crn.api.core.speed;

import java.util.List;

/**
 * Supplies the speed restrictions that apply on the track ahead of a train, so that the backend can
 * account for them when it estimates travel times.
 * <p>
 * Implement this if your mod restricts train speed by means the backend cannot observe on its own,
 * then register it with {@link SpeedLimitProviderRegistry}. Without a provider the backend assumes
 * a train may run at its own top speed throughout.
 * <p>
 * Implementations are called during time estimation, may run off the server thread and may be
 * called often, so they must be quick, must not block and must tolerate concurrent calls. An
 * implementation that throws is skipped for that query.
 */
@FunctionalInterface
public interface ISpeedLimitProvider {

    /**
     * The restrictions that apply within the queried distance, which need not be sorted and may be
     * empty. Segments beyond the query's horizon are discarded. Returning {@code null} is treated
     * as no restrictions.
     */
    List<SpeedLimitSegment> getSpeedLimits(SpeedLimitQuery query);

    /**
     * Whether this provider has anything to say about the query at all. Overriding this to reject
     * queries early is cheaper than returning an empty list from
     * {@link #getSpeedLimits(SpeedLimitQuery)}.
     */
    default boolean appliesTo(SpeedLimitQuery query) {
        return true;
    }
}
