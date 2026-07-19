package de.mrjulsen.crn.navigator.route;

/**
 * How likely a change of trains is to actually work out.
 * <p>
 * Every change a route search returns is possible according to the current projection - the
 * connecting train does leave after the feeding one arrives, with the requested transfer time to
 * spare. This says how much of that is left over once the feeding train's present delay is taken
 * into account a second time, i.e. what happens if it loses the same time again.
 */
public enum TransferRisk {

    /** Comfortable: the change still works even if the feeding train loses more time. */
    SAFE,

    /** Tight: it works now, but a further delay of the feeding train would break it. */
    TIGHT,

    /** Risky: only the requested minimum is available, with nothing to absorb a further delay. */
    RISKY;

    /** Whether a traveller should be warned about this change. */
    public boolean isWarning() {
        return this != SAFE;
    }

    /** The more worrying of two risks. */
    public TransferRisk worse(TransferRisk other) {
        return other == null || compareTo(other) >= 0 ? this : other;
    }
}
