package de.mrjulsen.crn.network.packets.cts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.core.navigation.Edge;
import de.mrjulsen.crn.core.navigation.Graph;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TestRequestPacket implements IPacketBase<TestRequestPacket> {


    public TestRequestPacket() { }

    @Override
    public void encode(TestRequestPacket packet, FriendlyByteBuf buffer) {
    }

    @Override
    public TestRequestPacket decode(FriendlyByteBuf buffer) {
        return new TestRequestPacket();
    }

    @Override
    public void handle(TestRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            long startTime = System.currentTimeMillis();

            Graph graph = new Graph(context.get().getSender().getLevel().getDayTime());
            graph.schedulesById.values().stream().findFirst().get().print();

            List<TrainStationAlias> route = graph.navigate(
                GlobalSettingsManager.getInstance().getSettingsData().getAliasList().stream().filter(x -> x.getAliasName().equals(AliasName.of("Bärlingen (U)"))).findFirst().get(),
                GlobalSettingsManager.getInstance().getSettingsData().getAliasList().stream().filter(x -> x.getAliasName().equals(AliasName.of("Halloween-Stadtviertel"))).findFirst().get()
            );
            for (TrainStationAlias alias : route) {
                System.out.print(alias.getAliasName() + " -> ");
            }
            System.out.println();

            long estimatedTime = System.currentTimeMillis() - startTime;
            ModMain.LOGGER.info(String.format("Route calculated. Took %sms.",
                estimatedTime
            ));
            for (Edge edge : graph.getConnectionsFrom(graph.nodesById.values().stream().filter(x -> x.getStationAlias().getAliasName().equals(AliasName.of("Salzingen-Tannenberg"))).findFirst().get()).values()) {
                System.out.println(String.format(" -> %s - %s (%s)", graph.nodesById.get(edge.getFirstNodeId()), graph.nodesById.get(edge.getSecondNodeId()), edge.getCost()));
            }
        });
        
        context.get().setPacketHandled(true);
    }
    
    public static record StationData(Collection<String> stationName, Collection<Integer> indices, UUID trainId) {}
}
