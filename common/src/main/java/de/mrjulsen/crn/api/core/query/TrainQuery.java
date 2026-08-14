package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.train.TrainLifecycleState;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A filter over the tracked trains. Each field that is set narrows the result, and a field left
 * empty or {@code null} imposes no constraint. Build one from {@link #all()} and the {@code with...}
 * methods; instances are immutable, so each method returns a new query.
 *
 * @param reportable    If set, keeps only trains that are, or are not, fit to be shown publicly.
 * @param lines         If non-empty, keeps only trains working one of these lines now.
 * @param categories    If non-empty, keeps only trains running under one of these categories now.
 * @param trainName     If non-empty, keeps only trains with exactly this name.
 * @param delayedOnly   If set, keeps only trains that are, or are not, running late.
 * @param cancelledOnly If set, keeps only trains that are, or are not, out of service.
 * @param owners        If non-empty, keeps only trains owned by one of these players.
 * @param sessionIds    If non-empty, keeps only trains with one of these tracking sessions.
 * @param state         If set, keeps only trains in this lifecycle state.
 * @param serviceState  If set, keeps only trains in this service state.
 * @param liveState     If set, keeps only trains in this live state.
 */
@QueryModel
public record TrainQuery(
    Boolean reportable,
    Set<UUID> lines,
    Set<UUID> categories,
    String trainName,
    Boolean delayedOnly,
    Boolean cancelledOnly,
    Set<UUID> owners,
    Set<UUID> sessionIds,
    TrainLifecycleState state,
    ServiceState serviceState,
    LiveTrainState liveState
) {

    /** A query that keeps every tracked train, as a starting point for refinement. */
    public static TrainQuery all() {
        return new TrainQuery(null, Set.of(), Set.of(), "", null, null, Set.of(), Set.of(), null, null, null);
    }

    /** Keeps only trains that are, or are not, fit to be shown publicly. */
    @QueryParam(value = "reportable")
    public TrainQuery withReportable(@Nullable Boolean reportable) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains working one of the given lines now. */
    @QueryParam(value = "lines")
    public TrainQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains running under one of the given categories now. */
    @QueryParam(value = "categories")
    public TrainQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains with exactly the given name. */
    @QueryParam(value = "name")
    public TrainQuery withTrainName(String trainName) {
        Objects.requireNonNull(trainName);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains that are, or are not, running late. */
    @QueryParam(value = "delayed_only")
    public TrainQuery withDelayedOnly(@Nullable Boolean delayedOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains that are, or are not, out of service. */
    @QueryParam(value = "cancelled_only")
    public TrainQuery withCancelledOnly(@Nullable Boolean cancelledOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains owned by one of the given players. */
    @QueryParam(value = "owners")
    public TrainQuery withOwner(Set<UUID> owners) {
        Objects.requireNonNull(owners);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains with one of the given tracking sessions. */
    @QueryParam(value = "session_ids")
    public TrainQuery withSessionId(Set<UUID> sessionIds) {
        Objects.requireNonNull(sessionIds);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains in the given lifecycle state. */
    @QueryParam(value = "state")
    public TrainQuery withState(TrainLifecycleState state) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains in the given service state. */
    @QueryParam(value = "service_state")
    public TrainQuery withServiceState(ServiceState serviceState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Keeps only trains in the given live state. */
    @QueryParam(value = "live_state")
    public TrainQuery withLiveState(LiveTrainState liveState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    /** Whether the given train passes this query. */
    public boolean accept(TrackedTrain train) {
        return  (reportable == null || train.isReportable() == reportable) &&
                (trainName.isEmpty() || train.getTrainName().equals(trainName)) &&
                (lines.isEmpty() || train.getCurrentSection().map(x -> lines.contains(x.getTrainLineId())).orElse(false)) &&
                (categories.isEmpty() || train.getCurrentSection().map(x -> categories.contains(x.getTrainCategoryId())).orElse(false)) &&
                (delayedOnly == null || train.isDelayed() == delayedOnly) &&
                (cancelledOnly == null || train.isCancelled() == cancelledOnly) &&
                (owners.isEmpty() || owners.contains(train.getTrain().owner)) &&
                (sessionIds.isEmpty() || sessionIds.contains(train.getSessionId())) &&
                (state == null || train.getLifecycleState() == state) &&
                (serviceState == null || train.getServiceState() == serviceState) &&
                (liveState == null || train.getLiveState() == liveState);
    }
}
