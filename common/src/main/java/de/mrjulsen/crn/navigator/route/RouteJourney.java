package de.mrjulsen.crn.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.StationRef;

/**
 * A complete travel plan from one station to another: the trains to take, in order, and the changes
 * between them.
 * <p>
 * {@code transfers} always has exactly one entry fewer than {@code legs}, the one at index
 * {@code i} describing the change from leg {@code i} to leg {@code i + 1}.
 *
 * @param legs      The rides that make up this journey, in travel order. Never empty.
 * @param transfers The changes between them, in travel order.
 */
public record RouteJourney(List<RouteLeg> legs, List<RouteTransfer> transfers) {

    public RouteJourney {
        legs = legs == null ? List.of() : List.copyOf(legs);
        transfers = transfers == null ? List.of() : List.copyOf(transfers);
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
     * How many times the traveller has to change trains. Staying seated while a train continues as a
     * different service does not count.
     */
    public int transferCount() {
        return (int) transfers.stream().filter(RouteTransfer::requiresChangingTrains).count();
    }

    /** Whether one train carries the traveller the whole way. */
    public boolean isDirect() {
        return transferCount() == 0;
    }

    /** How long the traveller spends waiting for connections, in ticks. */
    public long totalTransferTime() {
        return transfers.stream().mapToLong(RouteTransfer::duration).sum();
    }

    /** The most worrying change on this journey, or {@link TransferRisk#SAFE} if there is none. */
    public TransferRisk worstTransferRisk() {
        TransferRisk worst = TransferRisk.SAFE;
        for (RouteTransfer transfer : transfers) {
            worst = transfer.risk().worse(worst);
        }
        return worst;
    }

    /** Whether any change on this journey is worth warning the traveller about. */
    public boolean hasRiskyTransfer() {
        return worstTransferRisk().isWarning();
    }

    /** Every station the journey touches, in travel order and without repeats. */
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

    /** Whether the journey calls at the given station at any point. */
    public boolean callsAt(String stationName) {
        return legs.stream().anyMatch(leg -> leg.calls().stream().anyMatch(x -> x.stationName().equals(stationName)));
    }

    /** The ids of the trains used, in travel order and with repeats collapsed. */
    public List<UUID> trainIds() {
        return legs.stream().map(RouteLeg::trainId).distinct().toList();
    }

    /**
     * A value identifying this journey by the rides it consists of, for recognising two searches
     * that produced the same plan.
     */
    public String signature() {
        StringBuilder signature = new StringBuilder();
        for (RouteLeg leg : legs) {
            signature.append(leg.trainId()).append(':').append(leg.departure())
                .append('>').append(leg.arrival()).append('|');
        }
        return signature.toString();
    }

    @Override
    public String toString() {
        return origin().name() + " " + departure() + " -> " + destination().name() + " " + arrival()
            + " (" + transferCount() + " transfers)";
    }
}
