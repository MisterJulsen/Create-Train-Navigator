package de.mrjulsen.crn.api;

public interface IPredictableWaitCondition {

    /**
     * Calculate up to which time the train is expected to wait here.
     * @param worldTime The world time to which this condition will be triggered
     * @return The departure time of the train.
     */
    long waitUntil(long worldTime);
    default long waitMinUntil(long worldTime) {
        return waitUntil(worldTime);
    }
}
