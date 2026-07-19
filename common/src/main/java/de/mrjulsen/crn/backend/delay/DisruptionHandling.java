package de.mrjulsen.crn.backend.delay;

/**
 * What a reason that takes a train out of service means for the train's data - see
 * {@link DelayCause#disruptionHandling()}.
 * <p>
 * When several reasons apply at once, {@link #DELIBERATE} wins: a player who parks a train has said
 * what should happen to it, and that outranks whatever else the train may also be suffering from.
 */
public enum DisruptionHandling {

    /**
     * The train was taken out of service on purpose - its schedule was paused or removed because a
     * player no longer needs it. Travellers are told about it for a while, then the train is
     * forgotten entirely: its data would describe a service nobody intends to run any more.
     */
    DELIBERATE,

    /**
     * Something went wrong (a derailment, an unusable track network). Nobody asked for this, so the
     * train's data is kept even after it has stopped being displayed - it is what a player needs to
     * see what happened.
     */
    FAULT
}
