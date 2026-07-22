package de.mrjulsen.crn.client.journey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.client.RealtimeTrains;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.navigator.route.RouteTransfer;
import de.mrjulsen.crn.util.ModUtils;

/**
 * Keeps a {@link RouteJourney} up to date with what its trains are doing, and follows how far along
 * it the traveller has got.
 * <p>
 * A route is carried around like a bag: whoever holds it reads {@code call.realtime()} and gets the
 * current state without asking anyone. Somebody has to fill that bag, and this is it. It subscribes
 * to every train the journey uses, and on each poll writes the fresh times into the calls and the
 * fresh reasons into the legs' {@linkplain de.mrjulsen.crn.navigator.route.DelayLog delay logs}.
 * <p>
 * What is behind the traveller is left alone. A call whose train has moved on is sealed at the value
 * it last held - the delay it really had - because the same station coming round again belongs to a
 * journey the traveller is not on. The same goes one level up: once a leg has been ridden through,
 * its delay log is frozen and keeps the reasons that made it late.
 * <p>
 * Several trackers may share one journey without harm; they write the same values from the same
 * pool. What matters is that the journey <em>instance</em> is shared, so a saved route being watched
 * in the background and the same route opened in a window are one bag rather than two.
 */
public final class JourneyTracker implements AutoCloseable {

    /** What the tracker reports. Everything is optional to implement; take only what you show. */
    public interface Listener {
        default void onPhaseChanged(JourneyPhase phase, JourneyTracker tracker) {}
        default void onNextCallChanged(RouteCall call, JourneyTracker tracker) {}
        default void onUpdated(JourneyTracker tracker) {}
    }

    private final RouteJourney journey;
    private final List<Listener> listeners = new ArrayList<>();
    private RealtimeTrains.Watch watch;

    private boolean autoUpdate;
    private boolean started;

    private JourneyPhase phase = JourneyPhase.BEFORE_DEPARTURE;
    private int legIndex = 0;
    private RouteCall nextCall;

    /** A tracker that keeps the journey current on its own. */
    public JourneyTracker(RouteJourney journey) {
        this(journey, true);
    }

    /**
     * @param autoUpdate Whether to follow the trains by itself. A tracker without it only takes in
     *                   what {@link #refresh()} is called for, which is what a view wants when it is
     *                   showing a plan as it was rather than as it is.
     */
    public JourneyTracker(RouteJourney journey, boolean autoUpdate) {
        this.journey = journey;
        this.autoUpdate = autoUpdate;
        this.nextCall = journey.firstLeg().boarding();
    }

    public RouteJourney journey() {
        return journey;
    }

    public JourneyPhase phase() {
        return phase;
    }

    /** The leg the traveller is on, or the one they are heading for while changing. */
    public RouteLeg currentLeg() {
        return journey.legs().get(Math.min(legIndex, journey.legs().size() - 1));
    }

    public int currentLegIndex() {
        return legIndex;
    }

    /** The next call the traveller has ahead of them, or the final one once the journey is over. */
    public RouteCall nextCall() {
        return nextCall;
    }

    /** The change the traveller is making, if they are between trains. */
    public Optional<RouteTransfer> currentTransfer() {
        if (phase != JourneyPhase.TRANSFERRING || legIndex <= 0 || legIndex > journey.transfers().size()) {
            return Optional.empty();
        }
        return Optional.of(journey.transfers().get(legIndex - 1));
    }

    /** The live state of the train running the given leg, once a poll has come back. */
    public Optional<TrainSnapshot> trainOf(RouteLeg leg) {
        return RealtimeTrains.train(leg.trainId());
    }

    public boolean isAutoUpdating() {
        return autoUpdate;
    }

    /**
     * Whether any of this journey's trains has been rebuilt since the route was planned, which makes
     * the route a plan against a railway that no longer exists. Nothing is written into such a leg,
     * so what it shows stays what was true when the plan was still good.
     */
    public boolean isOutdated() {
        return journey.legs().stream().anyMatch(this::isOutdated);
    }

    /**
     * Whether the train running this leg is no longer the one the route was planned against.
     * <p>
     * A train keeps its id when it is taken apart and rebuilt, or when its learned timings are reset,
     * but everything measured about it starts over and its schedule may be a different one entirely.
     * The session says which run of the train the plan was made against, and a call is matched to the
     * live train by its position in the schedule - so writing across a change of session would put one
     * station's times into another station's call. A leg without a known session is left alone, since
     * a route from before sessions were recorded says nothing either way.
     */
    private boolean isOutdated(RouteLeg leg) {
        if (leg.sessionId() == null) {
            return false;
        }
        return RealtimeTrains.train(leg.trainId())
            .map(train -> train.sessionId() != null && !train.sessionId().equals(leg.sessionId()))
            .orElse(false);
    }

    /**
     * Turns following the trains on or off. Turning it off leaves the journey holding whatever it
     * last took in, so a view can stop the times moving under the traveller without losing them.
     */
    public void setAutoUpdate(boolean value) {
        if (autoUpdate == value) {
            return;
        }
        autoUpdate = value;
        if (!started) {
            return;
        }
        if (value) {
            acquireWatch();
        } else {
            releaseWatch();
        }
    }

    /** Begins tracking. Until this runs the journey holds what it was planned or loaded with. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        if (autoUpdate) {
            acquireWatch();
        }
        refresh();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Takes what the pool currently holds into the journey and works out where the traveller is.
     * Called for every poll while auto-updating, and callable by hand at any time.
     */
    public void refresh() {
        long now = ModUtils.getTransformedWorldTime();
        for (RouteLeg leg : journey.legs()) {
            applyTo(leg, now);
        }
        recomputeProgress();
        List.copyOf(listeners).forEach(x -> x.onUpdated(this));
    }

    @Override
    public void close() {
        releaseWatch();
        listeners.clear();
        started = false;
    }

    private void acquireWatch() {
        if (watch == null) {
            watch = RealtimeTrains.watch(journey.trainIds(), this::refresh);
        }
    }

    private void releaseWatch() {
        if (watch != null) {
            watch.release();
            watch = null;
        }
    }

    /**
     * Writes what the leg's train currently reports into the leg. Everything the train no longer
     * reports on - because it is not watched, has not been reached, or has dropped the stop - is left
     * as it is rather than cleared: the last thing known about a call is better than nothing at all,
     * and for a call already behind the traveller it is the whole point.
     */
    private void applyTo(RouteLeg leg, long now) {
        if (isOutdated(leg)) {
            return;
        }
        Optional<JourneySnapshot> snapshot = RealtimeTrains.journey(leg.trainId());
        if (snapshot.isPresent()) {
            for (RouteCall call : leg.calls()) {
                applyTo(leg, call, snapshot.get());
            }
        }
        RealtimeTrains.train(leg.trainId()).ifPresent(train -> leg.delays().update(train.delays(), now));
        if (isLegDone(leg)) {
            leg.delays().freeze(now);
        }
    }

    private void applyTo(RouteLeg leg, RouteCall call, JourneySnapshot snapshot) {
        Optional<StopSnapshot> stop = snapshot.stops().stream()
            .filter(x -> x.entryIndex() == call.entryIndex())
            .findFirst();
        if (stop.isEmpty()) {
            return;
        }
        if (stop.get().completedVisits() > call.cycle()) {
            call.markPassed();
            return;
        }
        RouteCall projected = RealtimeTrains.liveCall(leg, call);
        call.applyRealtime(projected.realtimeStation(), projected.realtime());
    }

    private void recomputeProgress() {
        JourneyPhase newPhase = computePhase();
        RouteCall newNextCall = computeNextCall();

        if (newPhase != phase) {
            phase = newPhase;
            List.copyOf(listeners).forEach(x -> x.onPhaseChanged(phase, this));
        }
        if (newNextCall != null && !newNextCall.equals(nextCall)) {
            nextCall = newNextCall;
            List.copyOf(listeners).forEach(x -> x.onNextCallChanged(nextCall, this));
        }
    }

    /**
     * Whether the given call already lies behind the traveller.
     * <p>
     * Progress along a leg only ever moves forward: a call counts as behind the traveller when it and
     * every call before it have been served. Judging each call on its own would let a single stop the
     * train happens to serve out of order - which a cyclic journey produces readily - drop stations
     * out of the middle of the list.
     */
    public boolean isCallDone(RouteLeg leg, RouteCall call) {
        int index = leg.calls().indexOf(call);
        return index >= 0 && index < servedCount(leg);
    }

    /** How many of the leg's calls, counting from the first, the train has left behind. */
    private int servedCount(RouteLeg leg) {
        List<RouteCall> calls = leg.calls();
        int served = 0;
        while (served < calls.size() && calls.get(served).passed()) {
            served++;
        }
        return served;
    }

    /**
     * Whether the traveller has ridden this leg through to the end.
     * <p>
     * Alighting counts only once boarding does. A train may well serve the alighting station before it
     * ever picks the traveller up - on a cyclic journey it comes round again, and the traveller may
     * still be waiting for it at the boarding station - so the arrival on its own says nothing about
     * where the traveller is.
     */
    private boolean isLegDone(RouteLeg leg) {
        return servedCount(leg) == leg.calls().size();
    }

    private JourneyPhase computePhase() {
        List<RouteLeg> legs = journey.legs();

        int current = legs.size() - 1;
        for (int i = 0; i < legs.size(); i++) {
            if (!isLegDone(legs.get(i))) {
                current = i;
                break;
            }
        }
        legIndex = current;

        RouteLeg leg = legs.get(current);

        if (isCancelled(leg)) {
            return JourneyPhase.TRAIN_CANCELLED;
        }
        if (!journey.isLegReachable(current)) {
            return JourneyPhase.CONNECTION_MISSED;
        }
        if (current == legs.size() - 1 && isLegDone(leg)) {
            return JourneyPhase.COMPLETED;
        }
        if (!isCallDone(leg, leg.boarding())) {
            return current == 0 ? JourneyPhase.BEFORE_DEPARTURE : JourneyPhase.TRANSFERRING;
        }
        return JourneyPhase.RIDING;
    }

    private boolean isCancelled(RouteLeg leg) {
        return RealtimeTrains.train(leg.trainId()).map(TrainSnapshot::isCancelled).orElse(leg.cancelled());
    }

    private RouteCall computeNextCall() {
        RouteLeg leg = currentLeg();
        int served = servedCount(leg);
        return served < leg.calls().size() ? leg.calls().get(served) : leg.alighting();
    }
}
