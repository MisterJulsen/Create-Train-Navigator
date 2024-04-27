package de.mrjulsen.crn.client;

import java.util.function.Supplier;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlayScreen;
import de.mrjulsen.crn.client.gui.screen.AdvancedDisplaySettingsScreen;
import de.mrjulsen.crn.client.gui.screen.LoadingScreen;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteOverlaySettingsScreen;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;

public class ClientWrapper {
    
    public static void showNavigatorGui(Level level) {
        DLScreen.setScreen(new LoadingScreen());
        GlobalSettingsManager.syncToClient(() -> {
            ClientTrainStationSnapshot.syncToClient(() -> {
                DLScreen.setScreen(new NavigatorScreen(level));
            });
        });
    }

    public static void showRouteOverlaySettingsGui(RouteDetailsOverlayScreen overlay) {
        DLScreen.setScreen(new RouteOverlaySettingsScreen(overlay));
    }

    public static void handleErrorMessagePacket(ServerErrorPacket packet, Supplier<PacketContext> ctx) {        
        Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, Constants.TEXT_SERVER_ERROR, new TextComponent(packet.message)));   
    }
    
    public static void showAdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        DLScreen.setScreen(new AdvancedDisplaySettingsScreen(blockEntity));
    }
}
