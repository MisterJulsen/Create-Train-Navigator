package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class RouteDetailsTransferWidget extends DLRenderable {

    private final MutableComponent transferText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.transfer");

    protected static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png");
    protected static final int GUI_TEXTURE_WIDTH = 256;
    protected static final int GUI_TEXTURE_HEIGHT = 256;
    protected static final int ENTRY_WIDTH = 225;

    private final MutableComponent textConnectionEndangered = ELanguage.translate("gui.createrailwaysnavigator.route_overview.connection_endangered").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
    private final MutableComponent textConnectionMissed = ELanguage.translate("gui.createrailwaysnavigator.route_overview.connection_missed").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);

    private final TransferConnection connection;

    public RouteDetailsTransferWidget(int x, int y, int width, TransferConnection connection) {
        super(x, y, width, 24);
        this.connection = connection;
    }

    @SuppressWarnings("resource")
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        
        long time = connection.getDepartureStation().getScheduledDepartureTime() - connection.getArrivalStation().getScheduledArrivalTime();
        GuiUtils.drawTexture(GUI, graphics, x(), y(), ENTRY_WIDTH, height(), 0, 155, ENTRY_WIDTH, height(), GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);

        if (connection.isConnectionMissed()) {
            ModGuiIcons.CROSS.render(graphics, x() + 24, y + 4);
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, x() + 28 + ModGuiIcons.ICON_SIZE + 2, y + 8, textConnectionMissed, 0xFFFFFFFF, EAlignment.LEFT, false);
        } else if (connection.isConnectionEndangered()) {
            ModGuiIcons.WARN.render(graphics, x() + 24, y + 4);
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, x() + 28 + ModGuiIcons.ICON_SIZE + 2, y + 8, textConnectionEndangered, 0xFFFFFFFF, EAlignment.LEFT, false);
        } else {
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, x() + 32, y + 8, TextUtils.text(transferText.getString() + " " + (time < 0 ? "" : "(" + TimeUtils.parseDuration(time) + ")")), 0xFFFFFF, EAlignment.LEFT, false);
        }
    }    
}
