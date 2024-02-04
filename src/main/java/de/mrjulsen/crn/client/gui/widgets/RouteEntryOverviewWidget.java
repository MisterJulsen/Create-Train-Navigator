package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

public class RouteEntryOverviewWidget extends Button {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;

    private static final int DISPLAY_WIDTH = 190;
    
    private final SimpleRoute route;
    private final NavigatorScreen parent;
    private final Level level;
    private final int lastRefreshedTime;

    private static final String transferText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.route_entry.transfer").getString();
    

    public RouteEntryOverviewWidget(NavigatorScreen parent, Level level, int lastRefreshedTime, int pX, int pY, SimpleRoute route, OnPress pOnPress) {
        super(pX, pY, 200, 48, new TextComponent(route.getName()), pOnPress);
        this.route = route;
        this.parent = parent;
        this.level = level;
        this.lastRefreshedTime = lastRefreshedTime;
    }


    @Override
    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new RouteDetailsScreen(parent, level, lastRefreshedTime, minecraft.font, route));
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        
        float l = isMouseOver(pMouseX, pMouseY) ? 0.2f : 0;
        RenderSystem.setShaderColor(1 + l, 1 + l, 1 + l, 1);
        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 0, WIDTH, HEIGHT);

        Minecraft minecraft = Minecraft.getInstance();
        SimpleRoutePart[] parts = route.getParts().toArray(SimpleRoutePart[]::new);
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        drawString(pPoseStack, minecraft.font, String.format("%s - %s | %s %s | %s",
            TimeUtils.parseTime(lastRefreshedTime + route.getStartStation().getTicks(), TimeFormat.HOURS_24),
            TimeUtils.parseTime(lastRefreshedTime + route.getEndStation().getTicks(), TimeFormat.HOURS_24),
            route.getTransferCount(),
            transferText,
            TimeUtils.parseDurationShort(route.getTotalDuration())
        ), x + 6, y + 5, 0xFFFFFF);

        int routePartWidth = DISPLAY_WIDTH / parts.length;
        String end = route.getEndStation().getStationName();
        int textW = shadowlessFont.width(end);
        
        for (int i = 0; i < parts.length; i++) {
            fill(pPoseStack, x + 5 + (i * routePartWidth) + 1, y + 20, x + 5 + ((i + 1) * routePartWidth + 1) - 2, y + 31, 0xFF393939);
        }

        pPoseStack.scale(0.75f, 0.75f, 0.75f);
        
        for (int i = 0; i < parts.length; i++) {
            drawCenteredString(pPoseStack, shadowlessFont, parts[i].getTrainName(), (int)((x + 5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)((y + 23) / 0.75f), 0xFFFFFF); 
        }

        drawString(pPoseStack, shadowlessFont, route.getStartStation().getStationName(), (int)((x + 6) / 0.75f), (int)((y + 36) / 0.75f), 0xDBDBDB);
        drawString(pPoseStack, shadowlessFont, end, (int)((x + 6 + DISPLAY_WIDTH) / 0.75f - textW - 5), (int)((y + 36) / 0.75f), 0xDBDBDB);
        
        float s = 1 / 0.75f;
        pPoseStack.scale(s, s, s);
    }
    
}
