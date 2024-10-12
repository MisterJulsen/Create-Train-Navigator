package de.mrjulsen.crn.event;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractBuiltInCRNEvent;

/**
 * Called after all CRN client events have been registered. From this point on, all CRN client events can be accessed without any problems.
 */
public final class CRNClientEventsRegistryEvent extends AbstractBuiltInCRNEvent<Runnable> {
    public void run() {
        listeners.values().forEach(Runnable::run);
    }
}
