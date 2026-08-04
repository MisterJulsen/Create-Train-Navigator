package de.mrjulsen.crn.core.navigator.route;

import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.timing.StopTimes;
import net.minecraft.nbt.CompoundTag;

public record RouteTransfer(RouteCall from, RouteCall to, long minTransferTime, long riskBuffer, boolean staysSeated) {

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

    public StationRef arrivalStation() {
        return from.station();
    }

    public StationRef departureStation() {
        return to.station();
    }

    public long arrival() {
        return from.realtime().arrival();
    }

    public long departure() {
        return to.realtime().departure();
    }

    public long duration() {
        return Math.max(0, departure() - arrival());
    }

    public long margin() {
        return staysSeated ? 0 : departure() - arrival() - minTransferTime;
    }

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

    public boolean isMissed() {
        return state() == TransferState.MISSED;
    }

    public boolean isEndangered() {
        return state() == TransferState.ENDANGERED;
    }

    public String arrivalStationName() {
        return arrivalStation().name();
    }

    public String departureStationName() {
        return departureStation().name();
    }

    public boolean changesStation() {
        return arrivalStation().isKnown() && departureStation().isKnown()
            && !arrivalStation().name().equals(departureStation().name());
    }

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

    private static final String NBT_MIN_TRANSFER_TIME = "MinTransferTime";
    private static final String NBT_RISK_BUFFER = "RiskBuffer";
    private static final String NBT_STAYS_SEATED = "StaysSeated";

    @Override
    public String toString() {
        return arrivalStation().name() + (changesStation() ? " -> " + departureStation().name() : "")
            + " +" + duration() + (staysSeated ? " (through)" : "") + " [" + state() + "]";
    }
}
