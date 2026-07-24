package de.mrjulsen.crn.client.journey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.api.core.JourneySnapshot;
import de.mrjulsen.crn.api.core.StopSnapshot;
import de.mrjulsen.crn.api.core.TrainSnapshot;
import de.mrjulsen.crn.client.RealtimeTrains;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;
import de.mrjulsen.crn.util.ModUtils;

public final class JourneyTracker implements AutoCloseable {

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

    public JourneyTracker(RouteJourney journey) {
        this(journey, true);
    }

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

    public RouteLeg currentLeg() {
        return journey.legs().get(Math.min(legIndex, journey.legs().size() - 1));
    }

    public int currentLegIndex() {
        return legIndex;
    }

    public RouteCall nextCall() {
        return nextCall;
    }

    public Optional<RouteTransfer> currentTransfer() {
        if (phase != JourneyPhase.TRANSFERRING || legIndex <= 0 || legIndex > journey.transfers().size()) {
            return Optional.empty();
        }
        return Optional.of(journey.transfers().get(legIndex - 1));
    }

    public Optional<TrainSnapshot> trainOf(RouteLeg leg) {
        return RealtimeTrains.train(leg.trainId());
    }

    public boolean isAutoUpdating() {
        return autoUpdate;
    }

    public boolean isOutdated() {
        return journey.legs().stream().anyMatch(this::isOutdated);
    }

    private boolean isOutdated(RouteLeg leg) {
        if (leg.sessionId() == null) {
            return false;
        }
        return RealtimeTrains.train(leg.trainId())
            .map(train -> train.sessionId() != null && !train.sessionId().equals(leg.sessionId()))
            .orElse(false);
    }

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

    public boolean isCallDone(RouteLeg leg, RouteCall call) {
        int index = leg.calls().indexOf(call);
        return index >= 0 && index < servedCount(leg);
    }

    private int servedCount(RouteLeg leg) {
        List<RouteCall> calls = leg.calls();
        int served = 0;
        while (served < calls.size() && calls.get(served).passed()) {
            served++;
        }
        return served;
    }

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
