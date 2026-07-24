package de.mrjulsen.crn.core.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.api.core.StationRef;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

public record RouteJourney(List<RouteLeg> legs, List<RouteTransfer> transfers) {

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

    public RouteLeg firstLeg() {
        return legs.get(0);
    }

    public RouteLeg lastLeg() {
        return legs.get(legs.size() - 1);
    }

    public StationRef origin() {
        return firstLeg().from();
    }

    public StationRef destination() {
        return lastLeg().to();
    }

    public long departure() {
        return firstLeg().departure();
    }

    public long arrival() {
        return lastLeg().arrival();
    }

    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    public int transferCount() {
        return transfers.size();
    }

    public boolean isDirect() {
        return transfers.isEmpty();
    }

    public long totalTransferTime() {
        return transfers.stream().mapToLong(RouteTransfer::duration).sum();
    }

    public TransferState worstTransferRisk() {
        TransferState worst = TransferState.SAFE;
        for (RouteTransfer transfer : transfers) {
            worst = transfer.state().worse(worst);
        }
        return worst;
    }

    public boolean hasRiskyTransfer() {
        return worstTransferRisk().isWarning();
    }

    public boolean isLegReachable(RouteLeg leg) {
        return isLegReachable(legs.indexOf(leg));
    }

    public boolean isLegReachable(int legIndex) {
        if (legIndex <= 0) {
            return true;
        }
        return transfers.subList(0, Math.min(legIndex, transfers.size())).stream()
            .allMatch(transfer -> transfer.state().isReachable());
    }

    public boolean isAnyCancelled() {
        return legs.stream().anyMatch(RouteLeg::cancelled);
    }

    public boolean hasDeparted(long now) {
        return departure() < now;
    }

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

    public boolean callsAt(String stationName) {
        return legs.stream().anyMatch(leg -> leg.calls().stream().anyMatch(x -> x.realtimeStationName().equals(stationName)));
    }

    public List<UUID> trainIds() {
        return legs.stream().map(RouteLeg::trainId).distinct().toList();
    }

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

    private static final String NBT_LEGS = "Legs";
    private static final String NBT_TRANSFERS = "Transfers";

    @Override
    public String toString() {
        return origin().name() + " " + departure() + " -> " + destination().name() + " " + arrival()
            + " (" + transferCount() + " transfers)";
    }
}
