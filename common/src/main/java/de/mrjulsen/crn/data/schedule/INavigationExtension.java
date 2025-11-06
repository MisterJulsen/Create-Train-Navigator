package de.mrjulsen.crn.data.schedule;

import java.util.Optional;

import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition.DelayedWaitConditionContext;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.mcdragonlib.util.Pair;

public interface INavigationExtension {
    void addDelayedWaitCondition(Pair<IDelayedWaitCondition, DelayedWaitConditionContext> pair);
    boolean isDelayedWaitConditionPending();
    Optional<PenaltyResult> getPenaltiesByDirection();
}
