package de.mrjulsen.crn.data.schedule;

/**
 * Implemented by a schedule wait condition that can say in advance when it will be satisfied, so
 * that departure times can be predicted instead of only observed.
 * <p>
 * Implement this on a custom wait condition whose end is known ahead of time. A condition that
 * cannot know this should not implement the interface; the backend then falls back to learning the
 * stop's duration from how long the train actually waits.
 */
public interface IPredictableWaitCondition {

    /**
     * The time this condition is expected to be satisfied, given the current time. Both are in the
     * same unit as {@link de.mrjulsen.crn.api.core.RailwayBackendApi#getCurrentTime()}.
     */
    long waitUntil(long worldTime);

    /**
     * The earliest time the condition could be satisfied, where that differs from the expected one.
     * A condition that may release the train early should report that here, so the train is not
     * held on the timetable longer than it needs to be.
     */
    default long waitMinUntil(long worldTime) {
        return waitUntil(worldTime);
    }
}
