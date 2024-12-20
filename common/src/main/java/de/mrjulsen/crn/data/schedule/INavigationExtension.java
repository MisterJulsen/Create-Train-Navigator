package de.mrjulsen.crn.data.schedule;

import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition.DelayedWaitConditionContext;
import de.mrjulsen.mcdragonlib.data.Pair;

public interface INavigationExtension {
    void addDelayedWaitCondition(Pair<IDelayedWaitCondition, DelayedWaitConditionContext> pair);
    boolean isDelayedWaitConditionPending();
}
