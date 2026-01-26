package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public class RouteDetailsTransferWidget extends DLGuiComponent {

    protected static final DLTexture GUI = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png"), 256, 256);
    protected static final int ENTRY_WIDTH = 225;

    private final MutableComponent textTransfer = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.transfer");
    private final MutableComponent textConnectionEndangered = CustomLanguage.translate("gui.createrailwaysnavigator.route_overview.connection_endangered").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
    private final MutableComponent textConnectionMissed = CustomLanguage.translate("gui.createrailwaysnavigator.route_overview.connection_missed").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);

    private final TransferConnection connection;

    public RouteDetailsTransferWidget(TransferConnection connection) {
        super(0, 0, ENTRY_WIDTH, 24);
        this.connection = connection;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        long time = connection.getDepartureStation().getScheduledDepartureTime() - connection.getArrivalStation().getScheduledArrivalTime();
        GuiUtils.drawTexture(GUI, graphics, 0, 0, width(), height(), 0, 155, ENTRY_WIDTH, height(), TextureFillMode.STRETCH);

        if (connection.isConnectionMissed()) {
            ModGuiIcons.CROSS.render(graphics, 24, 4);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 28 + ModGuiIcons.ICON_SIZE + 2, 8, textConnectionMissed, DLColor.WHITE, ETextAlignment.LEFT, false);
        } else if (connection.isConnectionEndangered()) {
            ModGuiIcons.WARN.render(graphics, 24, 4);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 28 + ModGuiIcons.ICON_SIZE + 2, 8, textConnectionEndangered, DLColor.WHITE, ETextAlignment.LEFT, false);
        } else {
            GuiUtils.drawString(graphics, graphics.defaultFont(), 32, 8, TextUtils.text(textTransfer.getString() + " " + (time < 0 ? "" : "(" + DLTime.fromTicks(time, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME) + ")")), DLColor.WHITE, ETextAlignment.LEFT, false);
        }
    }    
}
