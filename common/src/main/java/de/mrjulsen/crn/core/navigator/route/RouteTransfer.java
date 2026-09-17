package de.mrjulsen.crn.core.navigator.route;

import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.timing.StopTimes;
import net.minecraft.nbt.CompoundTag;

/**
 * One transfer between two consecutive legs of a journey: where travellers change, and how tight the
 * connection is.
 * <p>
 * Times are in game ticks on the backend's time base.
 *
 * @param from            The call the traveller arrives on.
 * @param to              The call the traveller departs on.
 * @param minTransferTime The least time needed to change trains here, in ticks.
 * @param riskBuffer      Extra time on top of that before the connection counts as safe, in ticks.
 * @param staysSeated     Whether travellers stay aboard the same train, so this is no real change.
 */
public record RouteTransfer(RouteCall from, RouteCall to, long minTransferTime, long riskBuffer, boolean staysSeated) {

    private static final String NBT_MIN_TRANSFER_TIME = "MinTransferTime";
    private static final String NBT_RISK_BUFFER = "RiskBuffer";
    private static final String NBT_STAYS_SEATED = "StaysSeated";

    private static final RouteCall UNLINKED = new RouteCall(StationRef.NONE, StationRef.NONE, -1, 0, StopTimes.UNKNOWN, StopTimes.UNKNOWN);

    public RouteTransfer {
        from = from == null ? UNLINKED : from;
        to = to == null ? UNLINKED : to;
        minTransferTime = Math.max(0, minTransferTime);
        riskBuffer = Math.max(0, riskBuffer);
    }

    public RouteTransfer(long minTransferTime, long riskBuffer, boolean staysSeated) {
        this(null, null, minTransferTime, riskBuffer, staysSeated);
    }

    public RouteTransfer linkedTo(RouteCall from, RouteCall to) {
        return new RouteTransfer(from, to, minTransferTime, riskBuffer, staysSeated);
    }

    /** The station the traveller arrives at. */
    public StationRef arrivalStation() {
        return from.station();
    }

    /** The station the traveller leaves from. */
    public StationRef departureStation() {
        return to.station();
    }

    /** When the feeder leg arrives. */
    public long arrival() {
        return from.realtime().arrival();
    }

    /** When the onward leg departs. */
    public long departure() {
        return to.realtime().departure();
    }

    /** How long the transfer lasts, in ticks. */
    public long duration() {
        return Math.max(0, departure() - arrival());
    }

    /** The spare time beyond the minimum transfer time, in ticks; negative when there is too little. */
    public long margin() {
        return staysSeated ? 0 : departure() - arrival() - minTransferTime;
    }

    /** How safe the transfer is; see {@link TransferState}. */
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

    /** Whether the connecting train has already left. */
    public boolean isMissed() {
        return state() == TransferState.MISSED;
    }

    /** Whether the transfer can no longer be made as things stand. */
    public boolean isEndangered() {
        return state() == TransferState.ENDANGERED;
    }

    /** The name of the arrival station. */
    public String arrivalStationName() {
        return arrivalStation().name();
    }

    /** The name of the departure station. */
    public String departureStationName() {
        return departureStation().name();
    }

    /** Whether travellers change to a different station, not just a different platform. */
    public boolean changesStation() {
        return arrivalStation().isKnown() && departureStation().isKnown()
            && !arrivalStation().name().equals(departureStation().name());
    }

    /** Whether the change is one travellers would notice by the displayed name. */
    public boolean changesTag() {
        return changesStation() && !arrivalStation().displayName().equals(departureStation().displayName());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_MIN_TRANSFER_TIME, minTransferTime);
        nbt.putLong(NBT_RISK_BUFFER, riskBuffer);
        nbt.putBoolean(NBT_STAYS_SEATED, staysSeated);
        return nbt;
    }

    public static RouteTransfer fromNbt(CompoundTag nbt) {
        return new RouteTransfer(
            nbt.getLong(NBT_MIN_TRANSFER_TIME),
            nbt.getLong(NBT_RISK_BUFFER),
            nbt.getBoolean(NBT_STAYS_SEATED)
        );
    }

    @Override
    public String toString() {
        return arrivalStation().name() + (changesStation() ? " -> " + departureStation().name() : "") + " +" + duration() + (staysSeated ? " (through)" : "") + " [" + state() + "]";
    }
}
