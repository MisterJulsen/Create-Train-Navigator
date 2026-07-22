package de.mrjulsen.crn.navigator.route;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.timing.StopTimes;
import net.minecraft.nbt.CompoundTag;

/**
 * The gap between two legs of a route: where the traveller gets out, where they get back in, how
 * long they have, and whether that is going to work.
 * <p>
 * Nothing here is stored twice. The transfer points at the call the feeding train arrives on and the
 * call the connecting train leaves from, and everything else follows from those two - which is what
 * makes it live: as the trains report new times into those calls, the transfer time shrinks and the
 * {@linkplain #state() state} moves along with them.
 * <p>
 * Getting out and getting back in are two different places. Both trains only have to serve the same
 * station <em>tag</em>, so a change within one tag can still mean walking from one track station to
 * another - and with a waypoint in the route the two need not even share a tag. Hence two calls
 * rather than one; {@link #changesStation()} says whether they actually differ.
 *
 * @param from            The call the feeding train arrives on, i.e. where the traveller gets out.
 * @param to              The call the connecting train leaves from.
 * @param minTransferTime How long the change needs at minimum, from the query it was searched with.
 * @param riskBuffer      How much slack the change wants beyond the feeding train's present delay
 *                        before it counts as comfortable. Also from the query.
 * @param staysSeated     Whether this is physically the same train, continuing under a different
 *                        service or starting its next run. It is still a change - one service ends
 *                        and another begins - but the traveller may make it without leaving their
 *                        seat, so no walking is involved, no minimum transfer time applies, and it
 *                        cannot be missed.
 */
public record RouteTransfer(RouteCall from, RouteCall to, long minTransferTime, long riskBuffer, boolean staysSeated) {

    /**
     * Stands in for a call until {@link RouteJourney} links the transfer to the legs it sits between.
     * A transfer is planned before the leg it leads to exists, so it is built without its calls and
     * wired up when the journey is assembled.
     */
    private static final RouteCall UNLINKED = new RouteCall(StationRef.NONE, StationRef.NONE, -1, 0, StopTimes.UNKNOWN, StopTimes.UNKNOWN);

    public RouteTransfer {
        from = from == null ? UNLINKED : from;
        to = to == null ? UNLINKED : to;
        minTransferTime = Math.max(0, minTransferTime);
        riskBuffer = Math.max(0, riskBuffer);
    }

    /** A change not yet attached to the calls it sits between. See {@link #linkedTo}. */
    public RouteTransfer(long minTransferTime, long riskBuffer, boolean staysSeated) {
        this(null, null, minTransferTime, riskBuffer, staysSeated);
    }

    /** This change as it sits between the given calls. */
    public RouteTransfer linkedTo(RouteCall from, RouteCall to) {
        return new RouteTransfer(from, to, minTransferTime, riskBuffer, staysSeated);
    }

    /** The station the traveller gets out at, as the feeding train really serves it. */
    public StationRef arrivalStation() {
        return from.realtimeStation();
    }

    /** The station the connecting train really leaves from. */
    public StationRef departureStation() {
        return to.realtimeStation();
    }

    /** When the feeding train gets in, in transformed game ticks. */
    public long arrival() {
        return from.realtime().arrival();
    }

    /** When the connecting train leaves. */
    public long departure() {
        return to.realtime().departure();
    }

    /** How long the traveller has for the change, in ticks. */
    public long duration() {
        return Math.max(0, departure() - arrival());
    }

    /** How much time is left over once the change itself is allowed for. Negative once out of reach. */
    public long margin() {
        return staysSeated ? 0 : departure() - arrival() - minTransferTime;
    }

    /**
     * How this change is doing as things stand.
     * <p>
     * Being out of reach and being lost are two different things: the connecting train is only
     * really gone once it has left, and until then it may yet be held or lose time itself. That is
     * what {@link RouteCall#passed()} on the departing call settles.
     */
    public TransferState state() {
        if (staysSeated) {
            return TransferState.SAFE;
        }
        long margin = margin();
        if (margin < 0) {
            return to.passed() ? TransferState.MISSED : TransferState.ENDANGERED;
        }
        long feederDelay = Math.max(0, from.arrivalDeviation());
        if (margin >= feederDelay + riskBuffer) {
            return TransferState.SAFE;
        }
        return margin >= feederDelay ? TransferState.TIGHT : TransferState.RISKY;
    }

    /** Whether the connecting train has gone without the traveller. */
    public boolean isMissed() {
        return state() == TransferState.MISSED;
    }

    /** Whether the change no longer works but has not been settled by the connecting train leaving. */
    public boolean isEndangered() {
        return state() == TransferState.ENDANGERED;
    }

    /** The name of the station the traveller gets out at. */
    public String arrivalStationName() {
        return arrivalStation().name();
    }

    /** The name of the station the connecting train leaves from. */
    public String departureStationName() {
        return departureStation().name();
    }

    /** Whether the traveller has to walk to a different station to catch the connecting train. */
    public boolean changesStation() {
        return arrivalStation().isKnown() && departureStation().isKnown()
            && !arrivalStation().name().equals(departureStation().name());
    }

    /** Whether that walk even leaves the station tag arrived in. */
    public boolean changesTag() {
        return changesStation() && !arrivalStation().displayName().equals(departureStation().displayName());
    }

    /**
     * Serializes this transfer. Only what the change itself was searched with is written - the calls
     * are part of the legs and are linked back on when the journey is read.
     */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_MIN_TRANSFER_TIME, minTransferTime);
        nbt.putLong(NBT_RISK_BUFFER, riskBuffer);
        nbt.putBoolean(NBT_STAYS_SEATED, staysSeated);
        return nbt;
    }

    /** Deserializes a transfer written by {@link #toNbt()}. */
    public static RouteTransfer fromNbt(CompoundTag nbt) {
        return new RouteTransfer(
            nbt.getLong(NBT_MIN_TRANSFER_TIME),
            nbt.getLong(NBT_RISK_BUFFER),
            nbt.getBoolean(NBT_STAYS_SEATED)
        );
    }

    private static final String NBT_MIN_TRANSFER_TIME = "MinTransferTime";
    private static final String NBT_RISK_BUFFER = "RiskBuffer";
    private static final String NBT_STAYS_SEATED = "StaysSeated";

    @Override
    public String toString() {
        return arrivalStation().name() + (changesStation() ? " -> " + departureStation().name() : "")
            + " +" + duration() + (staysSeated ? " (through)" : "") + " [" + state() + "]";
    }
}
