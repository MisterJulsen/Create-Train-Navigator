package de.mrjulsen.crn.client;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.NavigatorToast;
import de.mrjulsen.crn.client.gui.screen.AdvancedDisplaySettingsScreen;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.TrainDebugScreen;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ClientWrapper {
    
    private static ELanguage currentLanguage;
    private static Language currentClientLanguage;
    
    public static void showNavigatorGui() {
        DLScreen.setScreen(new NavigatorScreen(null));
    }

    @SuppressWarnings("resource")
    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    public static void handleErrorMessagePacket(ServerErrorPacket packet, Supplier<PacketContext> ctx) {        
        Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, Constants.TEXT_SERVER_ERROR, TextUtils.text(packet.message)));   
    }
    
    public static void showAdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        DLScreen.setScreen(new AdvancedDisplaySettingsScreen(blockEntity));
    }

    public static void updateLanguage(ELanguage lang, boolean force) {
        if (currentLanguage == lang && !force) {
            return;
        }

        LanguageInfo info = lang == ELanguage.DEFAULT ? null : Minecraft.getInstance().getLanguageManager().getLanguage(lang.getCode());
        if (info == null) {
            info = Minecraft.getInstance().getLanguageManager().getLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
        }
        currentLanguage = lang;
        if (lang == ELanguage.DEFAULT || info == null) {
            currentClientLanguage = Language.getInstance();
            CreateRailwaysNavigator.LOGGER.info("Updated custom language to: (Default)");
        } else {
            currentClientLanguage = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(lang == ELanguage.DEFAULT ? Minecraft.getInstance().getLanguageManager().getSelected() : lang.getCode()), false);
            CreateRailwaysNavigator.LOGGER.info("Updated custom language to: " + (info == null ? null : info.name()));
        }
    }

    public static Language getCurrentClientLanguage() {
        return currentClientLanguage == null ? Language.getInstance() : currentClientLanguage;
    }

    
    public static void sendCRNNotification(Component title, Component description) {
        if (ModClientConfig.ROUTE_NOTIFICATIONS.get()) {
            Minecraft.getInstance().getToasts().addToast(NavigatorToast.multiline(title, description));
        }
    }

    public static int renderMultilineLabelSafe(Graphics graphics, int x, int y, Font font, Component text, int maxWidth, int color) {
        MultiLineLabel label = MultiLineLabel.create(font, text, maxWidth);
        label.renderLeftAlignedNoShadow(graphics.graphics(), x, y, font.lineHeight, color);
        return font.lineHeight * label.getLineCount();
    }

    public static int getTextBlockHeight(Font font, Component text, int maxWidth) {
        int lines = font.split(text, maxWidth).size();
        return lines * font.lineHeight;
    }

    public static void showTrainDebugScreen() {
        RenderSystem.recordRenderCall(() -> {
            DLScreen.setScreen(new TrainDebugScreen(null));
        });
    }
}
