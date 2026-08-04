package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.train.TrainLifecycleState;
import de.mrjulsen.crn.web.annotation.RestQueryModel;
import de.mrjulsen.crn.web.annotation.RestQueryParam;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@RestQueryModel
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
    LiveTrainState liveState,
    Predicate<TrackedTrain> custom
) {

    @RestQueryParam(value = "reportable")
    public TrainQuery withReportable(@Nullable Boolean reportable) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "lines")
    public TrainQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "categories")
    public TrainQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "name")
    public TrainQuery withTrainName(String trainName) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "delayed_only")
    public TrainQuery withDelayedOnly(@Nullable Boolean delayedOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "cancelled_only")
    public TrainQuery withCancelledOnly(@Nullable Boolean cancelledOnly) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "owners")
    public TrainQuery withOwner(Set<UUID> owners) {
        Objects.requireNonNull(owners);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "session_ids")
    public TrainQuery withSessionId(Set<UUID> sessionIds) {
        Objects.requireNonNull(sessionIds);
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "state")
    public TrainQuery withState(TrainLifecycleState state) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "service_state")
    public TrainQuery withServiceState(ServiceState serviceState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    @RestQueryParam(value = "live_state")
    public TrainQuery withLiveState(LiveTrainState liveState) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
    }

    public TrainQuery withCustom(Predicate<TrackedTrain> custom) {
        return new TrainQuery(reportable, lines, categories, trainName, delayedOnly, cancelledOnly, owners, sessionIds, state, serviceState, liveState, custom);
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
                (liveState == null || train.getLiveState() == liveState) &&
                (custom == null || custom.test(train));
    }
}
