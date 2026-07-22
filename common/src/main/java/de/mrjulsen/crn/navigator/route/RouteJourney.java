package de.mrjulsen.crn.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * A complete travel plan from one station to another: the trains to take, in order, and the changes
 * between them.
 * <p>
 * {@code transfers} always has exactly one entry fewer than {@code legs}, the one at index
 * {@code i} describing the change from leg {@code i} to leg {@code i + 1}. That is what lets a
 * transfer be built without knowing the calls it sits between - assembling the journey is what wires
 * it to them, here and nowhere else.
 *
 * @param legs      The rides that make up this journey, in travel order. Never empty.
 * @param transfers The changes between them, in travel order.
 */
public record RouteJourney(List<RouteLeg> legs, List<RouteTransfer> transfers) {

    public RouteJourney {
        legs = legs == null ? List.of() : List.copyOf(legs);
        transfers = transfers == null ? List.of() : linked(legs, transfers);
    }

    /**
     * Every transfer attached to the calls it sits between. Idempotent, so a journey assembled from
     * the parts of others ends up wired to its own legs rather than to theirs.
     */
    private static List<RouteTransfer> linked(List<RouteLeg> legs, List<RouteTransfer> transfers) {
        List<RouteTransfer> result = new ArrayList<>(transfers.size());
        for (int i = 0; i < transfers.size(); i++) {
            result.add(i + 1 < legs.size()
                ? transfers.get(i).linkedTo(legs.get(i).alighting(), legs.get(i + 1).boarding())
                : transfers.get(i));
        }
        return List.copyOf(result);
    }

    /** The first leg of the journey. */
    public RouteLeg firstLeg() {
        return legs.get(0);
    }

    /** The last leg of the journey. */
    public RouteLeg lastLeg() {
        return legs.get(legs.size() - 1);
    }

    /** Where the journey starts. */
    public StationRef origin() {
        return firstLeg().from();
    }

    /** Where the journey ends. */
    public StationRef destination() {
        return lastLeg().to();
    }

    /** When the traveller leaves, in transformed game ticks. */
    public long departure() {
        return firstLeg().departure();
    }

    /** When the traveller arrives, in transformed game ticks. */
    public long arrival() {
        return lastLeg().arrival();
    }

    /** How long the whole journey takes from departure to arrival, in ticks. */
    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    /**
     * How many times the traveller has to change.
     * <p>
     * Every change counts, including one made without leaving one's seat: a service that ends and
     * hands over to another - or to its own next run - is two journeys however comfortable the change
     * between them is, and a traveller told "no changes" and then carried past the terminus of the
     * service they boarded has been told the wrong thing. {@link RouteTransfer#staysSeated()} is what
     * says how easy a particular one is.
     */
    public int transferCount() {
        return transfers.size();
    }

    /** Whether one service carries the traveller the whole way. */
    public boolean isDirect() {
        return transfers.isEmpty();
    }

    /** How long the traveller spends waiting for connections, in ticks. */
    public long totalTransferTime() {
        return transfers.stream().mapToLong(RouteTransfer::duration).sum();
    }

    /** The most worrying change on this journey, or {@link TransferState#SAFE} if there is none. */
    public TransferState worstTransferRisk() {
        TransferState worst = TransferState.SAFE;
        for (RouteTransfer transfer : transfers) {
            worst = transfer.state().worse(worst);
        }
        return worst;
    }

    /** Whether any change on this journey is worth warning the traveller about. */
    public boolean hasRiskyTransfer() {
        return worstTransferRisk().isWarning();
    }

    /**
     * Whether the traveller can still catch the given leg, i.e. whether every change leading up to it
     * can be made. One change out of reach strands the traveller, so everything from there on is out
     * of reach too, no matter how the later trains run.
     */
    public boolean isLegReachable(RouteLeg leg) {
        return isLegReachable(legs.indexOf(leg));
    }

    /** Whether the leg at the given position is still reachable. See {@link #isLegReachable(RouteLeg)}. */
    public boolean isLegReachable(int legIndex) {
        if (legIndex <= 0) {
            return true;
        }
        return transfers.subList(0, Math.min(legIndex, transfers.size())).stream()
            .allMatch(transfer -> transfer.state().isReachable());
    }

    /** Whether any train on this journey was cancelled when the route was searched. */
    public boolean isAnyCancelled() {
        return legs.stream().anyMatch(RouteLeg::cancelled);
    }

    /** Whether the first train has already left, which makes the journey no longer catchable. */
    public boolean hasDeparted(long now) {
        return departure() < now;
    }

    /** Every station the journey touches, in travel order and without repeats. */
    public List<StationRef> stations() {
        List<StationRef> stations = new ArrayList<>();
        for (RouteLeg leg : legs) {
            for (RouteCall call : leg.calls()) {
                if (stations.isEmpty() || !stations.get(stations.size() - 1).name().equals(call.realtimeStationName())) {
                    stations.add(call.realtimeStation());
                }
            }
        }
        return stations;
    }

    /** Whether the journey calls at the given station at any point. */
    public boolean callsAt(String stationName) {
        return legs.stream().anyMatch(leg -> leg.calls().stream().anyMatch(x -> x.realtimeStationName().equals(stationName)));
    }

    /** The ids of the trains used, in travel order and with repeats collapsed. */
    public List<UUID> trainIds() {
        return legs.stream().map(RouteLeg::trainId).distinct().toList();
    }

    /**
     * A value identifying this journey by the rides it consists of, for recognising two searches
     * that produced the same plan.
     * <p>
     * Built from the timetable rather than from what the trains are doing: the same plan is the same
     * plan whether or not its trains happen to be running late at the moment it is looked at.
     */
    public String signature() {
        StringBuilder signature = new StringBuilder();
        for (RouteLeg leg : legs) {
            signature.append(leg.trainId()).append(':').append(leg.boarding().scheduled().departure())
                .append('>').append(leg.alighting().scheduled().arrival()).append('|');
        }
        return signature.toString();
    }

    /** Serializes this journey. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LEGS, NbtHelper.writeList(legs, RouteLeg::toNbt));
        nbt.put(NBT_TRANSFERS, NbtHelper.writeList(transfers, RouteTransfer::toNbt));
        return nbt;
    }

    /** Deserializes a journey written by {@link #toNbt()}. */
    public static RouteJourney fromNbt(CompoundTag nbt) {
        return new RouteJourney(
            NbtHelper.readList(nbt, NBT_LEGS, RouteLeg::fromNbt),
            NbtHelper.readList(nbt, NBT_TRANSFERS, RouteTransfer::fromNbt)
        );
    }

    private static final String NBT_LEGS = "Legs";
    private static final String NBT_TRANSFERS = "Transfers";

    @Override
    public String toString() {
        return origin().name() + " " + departure() + " -> " + destination().name() + " " + arrival()
            + " (" + transferCount() + " transfers)";
    }
}
