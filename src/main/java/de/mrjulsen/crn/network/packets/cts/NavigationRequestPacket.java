package de.mrjulsen.crn.network.packets.cts;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.core.Navigation;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.Route;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.NavigationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class NavigationRequestPacket implements IPacketBase<NavigationRequestPacket> {

    public long id;
    public String start;
    public String end;
    public UserSettings filterSettings;

    public NavigationRequestPacket() { }
    
    public NavigationRequestPacket(long id, String start, String end) {
        this(id, start, end, new UserSettings());
    }

    private NavigationRequestPacket(long id, String start, String end, UserSettings settings) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.filterSettings = settings;
    }

    @Override
    public void encode(NavigationRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeUtf(packet.start);
        buffer.writeUtf(packet.end);
        buffer.writeNbt(packet.filterSettings.toNbt());
    }

    @Override
    public NavigationRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        String start = buffer.readUtf();
        String end = buffer.readUtf();
        UserSettings filterSettings = UserSettings.fromNbt(buffer.readNbt());
        return new NavigationRequestPacket(id, start, end, filterSettings);
    }

    @Override
    public void handle(NavigationRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            Thread navigationThread = new Thread(() -> {
                List<Route> routes = new ArrayList<>();
                
                try {
                    TrainStationAlias startAlias = GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(packet.start);
                    TrainStationAlias endAlias = GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(packet.end);

                    if (startAlias == null || endAlias == null) {
                        return;
                    }

                    routes = packet.filterSettings.shouldOnlyTakeNextDepartingTrain() ? 
                        Navigation.navigateForNext(startAlias, endAlias, packet.filterSettings) :
                        Navigation.navigateForAll(startAlias, endAlias, packet.filterSettings);
                    
                } catch (Exception e) {
                    ModMain.LOGGER.error("Navigation error: ", e);
                    NetworkManager.sendToClient(new ServerErrorPacket(e.getMessage()), context.get().getSender());
                } finally {                    
                    NetworkManager.sendToClient(new NavigationResponsePacket(packet.id, new ArrayList<>(routes.stream().map(x -> new SimpleRoute(x)).toList())), context.get().getSender());
                }                
            });
            navigationThread.setPriority(Thread.MIN_PRIORITY);
            navigationThread.setName("Navigation Worker");
            navigationThread.start();

        });
        
        context.get().setPacketHandled(true);      
    }    
}
