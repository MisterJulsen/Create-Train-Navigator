package de.mrjulsen.crn.api;

public interface IPredictableWaitCondition {

    /**
     * Calculates the time the train departs here.
     * @param worldTime The world time to which this condition will be triggered
     * @return The departure time of the train.
     */
    long waitUntil(long worldTime);
    
    /**
     * Calculates the minimum time the train can depart here.
     * @param worldTime The world time to which this condition will be triggered
     * @return The minimum departure time of the train.
     */
    default long waitMinUntil(long worldTime) {
        return waitUntil(worldTime);
    }
}
