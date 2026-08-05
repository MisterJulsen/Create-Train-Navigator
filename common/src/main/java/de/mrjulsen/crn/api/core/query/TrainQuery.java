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
    
    public static TrainQuery all() {
        return new TrainQuery(null, Set.of(), Set.of(), "", null, null, Set.of(), Set.of(), null, null, null);
    }

    @QueryParam(value = "reportable")
    public TrainQuery withReportable(@Nullable Boolean reportable) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "lines")
    public TrainQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "categories")
    public TrainQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "name")
    public TrainQuery withTrainName(String trainName) {
        Objects.requireNonNull(trainName);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "delayed_only")
    public TrainQuery withDelayedOnly(@Nullable Boolean delayedOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "cancelled_only")
    public TrainQuery withCancelledOnly(@Nullable Boolean cancelledOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "owners")
    public TrainQuery withOwner(Set<UUID> owners) {
        Objects.requireNonNull(owners);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "session_ids")
    public TrainQuery withSessionId(Set<UUID> sessionIds) {
        Objects.requireNonNull(sessionIds);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "state")
    public TrainQuery withState(TrainLifecycleState state) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "service_state")
    public TrainQuery withServiceState(ServiceState serviceState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

    @QueryParam(value = "live_state")
    public TrainQuery withLiveState(LiveTrainState liveState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState);
    }

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
