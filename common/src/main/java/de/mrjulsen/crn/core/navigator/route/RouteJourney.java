package de.mrjulsen.crn.core.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * One journey found by a route search: the legs travelled aboard trains, and the transfers between
 * them. A direct journey has one leg and no transfers.
 * <p>
 * Times are in game ticks on the backend's time base.
 *
 * @param legs      The legs travelled, in order.
 * @param transfers The transfers between consecutive legs; one fewer than the number of legs.
 */
public record RouteJourney(List<RouteLeg> legs, List<RouteTransfer> transfers) {

    private static final String NBT_LEGS = "Legs";
    private static final String NBT_TRANSFERS = "Transfers";

    public RouteJourney {
        legs = legs == null ? List.of() : List.copyOf(legs);
        transfers = transfers == null ? List.of() : linked(legs, transfers);
    }

    private static List<RouteTransfer> linked(List<RouteLeg> legs, List<RouteTransfer> transfers) {
        List<RouteTransfer> result = new ArrayList<>(transfers.size());
        for (int i = 0; i < transfers.size(); i++) {
            result.add(i + 1 < legs.size()
                ? transfers.get(i).linkedTo(legs.get(i).alighting(), legs.get(i + 1).boarding())
                : transfers.get(i));
        }
        return List.copyOf(result);
    }

    /** The journey's first leg. */
    public RouteLeg firstLeg() {
        return legs.get(0);
    }

    /** The journey's last leg. */
    public RouteLeg lastLeg() {
        return legs.get(legs.size() - 1);
    }

    /** The station the journey starts at. */
    public StationRef origin() {
        return firstLeg().from();
    }

    /** The station the journey ends at. */
    public StationRef destination() {
        return lastLeg().to();
    }

    /** When the journey departs. */
    public long departure() {
        return firstLeg().departure();
    }

    /** When the journey arrives. */
    public long arrival() {
        return lastLeg().arrival();
    }

    /** How long the whole journey takes, in ticks. */
    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    /** How many transfers the journey has. */
    public int transferCount() {
        return transfers.size();
    }

    /** Whether the journey needs no transfer. */
    public boolean isDirect() {
        return transfers.isEmpty();
    }

    /** The total time spent transferring, in ticks. */
    public long totalTransferTime() {
        return transfers.stream().mapToLong(RouteTransfer::duration).sum();
    }

    /** The riskiest of the journey's transfers, or {@link TransferState#SAFE} where there are none. */
    public TransferState worstTransferRisk() {
        TransferState worst = TransferState.SAFE;
        for (RouteTransfer transfer : transfers) {
            worst = transfer.state().worse(worst);
        }
        return worst;
    }

    /** Whether any transfer is at risk of being missed. */
    public boolean hasRiskyTransfer() {
        return worstTransferRisk().isWarning();
    }

    /** Whether every transfer before the given leg can still be made. */
    public boolean isLegReachable(RouteLeg leg) {
        return isLegReachable(legs.indexOf(leg));
    }

    /** Whether every transfer before the leg at the given index can still be made. */
    public boolean isLegReachable(int legIndex) {
        if (legIndex <= 0) {
            return true;
        }
        return transfers.subList(0, Math.min(legIndex, transfers.size())).stream()
            .allMatch(transfer -> transfer.state().isReachable());
    }

    /** Whether any leg of the journey is cancelled. */
    public boolean isAnyCancelled() {
        return legs.stream().anyMatch(RouteLeg::cancelled);
    }

    /** Whether the journey has already departed by the given time. */
    public boolean hasDeparted(long now) {
        return departure() < now;
    }

    /** Every station the journey calls at, in order, without repeating consecutive ones. */
    public List<StationRef> stations() {
        List<StationRef> stations = new ArrayList<>();
        for (RouteLeg leg : legs) {
            for (RouteCall call : leg.calls()) {
                if (stations.isEmpty() || !stations.get(stations.size() - 1).name().equals(call.stationName())) {
                    stations.add(call.station());
                }
            }
        }
        return stations;
    }

    /** Whether the journey calls at the given station. */
    public boolean callsAt(String stationName) {
        return legs.stream().anyMatch(leg -> leg.calls().stream().anyMatch(x -> x.stationName().equals(stationName)));
    }

    /** The distinct trains the journey uses, in the order they are boarded. */
    public List<UUID> trainIds() {
        return legs.stream().map(RouteLeg::trainId).distinct().toList();
    }

    /** A key identifying the journey by its trains and their times, used to recognise duplicates. */
    public String signature() {
        StringBuilder signature = new StringBuilder();
        for (RouteLeg leg : legs) {
            signature.append(leg.trainId()).append(':').append(leg.boarding().scheduled().departure())
                .append('>').append(leg.alighting().scheduled().arrival()).append('|');
        }
        return signature.toString();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LEGS, NbtHelper.writeList(legs, RouteLeg::toNbt));
        nbt.put(NBT_TRANSFERS, NbtHelper.writeList(transfers, RouteTransfer::toNbt));
        return nbt;
    }

    public static RouteJourney fromNbt(CompoundTag nbt) {
        return new RouteJourney(
            NbtHelper.readList(nbt, NBT_LEGS, RouteLeg::fromNbt),
            NbtHelper.readList(nbt, NBT_TRANSFERS, RouteTransfer::fromNbt)
        );
    }


    @Override
    public String toString() {
        return origin().name() + " " + departure() + " -> " + destination().name() + " " + arrival() + " (" + transferCount() + " transfers)";
    }
}
