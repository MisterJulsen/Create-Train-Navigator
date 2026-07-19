package de.mrjulsen.crn.backend.api;

import java.util.List;

/**
 * Extension point for anything that restricts how fast a train may travel and wants the backend's
 * predictions to account for it.
 * <p>
 * Without this the backend can only see a train's current, possibly momentarily throttled speed,
 * and would extrapolate that single value across the whole remaining distance - producing wildly
 * inaccurate estimates for a train inside, or just past, a restricted stretch. A provider reports
 * the limits lying ahead so travel time can be integrated segment by segment instead.
 * <p>
 * Register an implementation once during mod initialization via
 * {@link SpeedLimitProviderRegistry#register(net.minecraft.resources.ResourceLocation,
 * ISpeedLimitProvider)}. Providers are consulted only while the backend has not yet learned a
 * reliable duration for the train's current leg; a learned duration already reflects any limits,
 * so there is nothing to gain from reporting them for well-known legs.
 * <p>
 * Providers are independent: several may report on the same stretch, and the most restrictive limit
 * in force at any point wins. A provider therefore never has to know about the others.
 *
 * <h2>Threading</h2>
 * Providers are always called on the server thread, so an implementation may freely read the
 * train's navigation, the track graph and the world. The backend does the rest of its prediction
 * work on a worker thread but never calls into this API from there.
 */
@FunctionalInterface
public interface ISpeedLimitProvider {

    /**
     * The speed limits this provider knows about along the queried stretch of the train's path.
     * <p>
     * Segments need not be sorted and may overlap with those of other providers. Return an empty
     * list, or {@code null}, when this provider has nothing to contribute for the given train.
     *
     * @param query What is being asked. See {@link SpeedLimitQuery}.
     */
    List<SpeedLimitSegment> getSpeedLimits(SpeedLimitQuery query);

    /**
     * Whether this provider has anything to say about the given train at all.
     * <p>
     * Checked before {@link #getSpeedLimits(SpeedLimitQuery)}, so a provider that only covers part
     * of the network can rule a train out cheaply instead of doing the full lookup. The default
     * accepts every train.
     */
    default boolean appliesTo(SpeedLimitQuery query) {
        return true;
    }
}
