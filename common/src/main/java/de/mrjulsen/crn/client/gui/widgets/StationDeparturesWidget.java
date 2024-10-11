package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.TrainJourneySreen;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem.ContextMenuItemData;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StationDeparturesWidget extends DLButton implements AutoCloseable {    

    public static final int HEADER_HEIGHT = 20;
    public static final int DEFAULT_LINE_HEIGHT = 12;
    public static final float DEFAULT_SCALE = 0.75f;


    private final MutableComponent connectionInPast = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    
    private final ClientRoute route;
    private final boolean arrival;

    public StationDeparturesWidget(Screen parent, StationDeparturesViewer viewer, int x, int y, int width, ClientRoute route, boolean arrival) {
        super(x, y, width, 32, TextUtils.empty(), (b) -> {
            DLScreen.setScreen(new TrainJourneySreen(parent, route, route.getStart().getTrainId()));
        });
        this.route = route;
        this.arrival = arrival;

        setRenderStyle(AreaStyle.FLAT);
        setMenu(new DLContextMenu(() -> GuiAreaDefinition.of(this), () -> new DLContextMenuItem.Builder()
            .add(new ContextMenuItemData(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.view_details"), Sprite.empty(), true, (b) -> onPress.onPress(b), null))
        ));
    }  

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.DARK.getColor());

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0x22FFFFFF);
        }

        final float scale = 0.75f;
        Component trainName = TextUtils.text(route.getStart().getTrainDisplayName()).withStyle(ChatFormatting.BOLD);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(x(), y(), 0);
        graphics.poseStack().scale(scale, scale, scale);        
        if (arrival) {
            AllIcons.I_CONFIG_OPEN.render(graphics.poseStack(), 8, 5);
        } else {
            AllIcons.I_CONFIG_BACK.render(graphics.poseStack(), 8, 5);
        }

        if (route.isAnyCancelled()) {
            GuiUtils.drawString(graphics, font, (int)((x + width() - 5) / scale), (int)((y() + 15) / scale), trainCanceled, Constants.COLOR_DELAYED, EAlignment.RIGHT, false);
        } else if (route.getStart().isDeparted()) {
            GuiUtils.drawString(graphics, font, (int)((x + width() - 5) / scale), (int)((y() + 15) / scale), connectionInPast, Constants.COLOR_DELAYED, EAlignment.RIGHT, false);
        }

        CreateDynamicWidgets.renderTextHighlighted(graphics, 30, 6, font, trainName, route.getStart().getTrainDisplayColor());
        graphics.poseStack().popPose();

        Component platformText = TextUtils.text(route.getStart().getRealTimeStationTag().info().platform());
        int platformTextWidth = font.width(platformText);
        final int maxStationNameWidth = width() - platformTextWidth - 15 - (int)((45 + font.width(trainName)) * scale);
        MutableComponent stationText = arrival ? TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", route.getEnd().getClientTag().tagName()) : TextUtils.text(route.getStart().getDisplayTitle());
        if (font.width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(font.substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        GuiUtils.drawString(graphics, font, x() + (int)((45 + font.width(trainName)) * scale), y() + 6, stationText, 0xFFFFFF, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, x() + width() - 6, y() + 6, platformText, 0xFFFFFF, EAlignment.RIGHT, false);
        GuiUtils.drawString(graphics, font, x() + (int)(30 * scale), y() + 20, ModUtils.formatTime(arrival ? route.getStart().getScheduledArrivalTime() : route.getStart().getScheduledDepartureTime(), false), 0xFFFFFF, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, x() + (int)(30 * scale) + 40, y() + 20, ModUtils.formatTime(arrival ? route.getStart().getRealTimeArrivalTime() : route.getStart().getRealTimeDepartureTime(), false), (arrival ? route.getStart().isArrivalDelayed() : route.getStart().isDepartureDelayed()) ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        
        
        //GuiUtils.drawString(graphics, font, x() + 6, y() + 5, route.getStart().getTag().getTagName().get(), 0xFFFFFFFF, EAlignment.LEFT, false);

    }

    @Override
    public void close() {
        route.closeAll();
    }
    
}
