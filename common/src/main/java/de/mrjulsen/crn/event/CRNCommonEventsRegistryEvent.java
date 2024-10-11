package de.mrjulsen.crn.event;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractBuiltInCRNEvent;

/**
 * Called after all CRN common events have been registered. From this point on, all CRN common events can be accessed without any problems.
 */
public final class CRNCommonEventsRegistryEvent extends AbstractBuiltInCRNEvent<Runnable> {
    public void run() {
        listeners.values().forEach(Runnable::run);
    }
}
