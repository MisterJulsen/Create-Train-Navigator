package de.mrjulsen.crn.client.gui.windows;

import java.util.UUID;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RouteDetailsViewer;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.client.Minecraft;

public class TrainJourneyWindow extends AbstractNavigatorScreen {

    private final ClientRoute route;
    private final ClientRoutePart part;

    private final RouteDetailsViewer viewer;

    public TrainJourneyWindow(DLWindowManager manager, ClientRoute route, UUID trainId) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.title"), ContainerColor.GRAY, BarColor.GOLD);
        this.route = route;
        this.part = route.getClientParts().stream().filter(x -> x.getTrainId().equals(trainId)).findFirst().orElse(route.getFirstClientPart());

        int dy = FooterSize.DEFAULT.size() + 32;
        viewer = addComponent(new RouteDetailsViewer(3, dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1));
        viewer.showTrainDetails.set(false);
        viewer.canExpandCollapse.set(false);
        viewer.expanded.set(true);
        viewer.showEntireJourney.set(true);
        viewer.displayPart(route, x -> x == part);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);

        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, 32, ContainerColor.BLUE);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 8, y + 7, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.date", (long)DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem()).toGameDays()), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 8, y + 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.train", part.getFirstStop().getTrainDisplayName(), part.getFirstStop().getTrainId().toString().split("-")[0], part.getFirstStop().getDisplayTitle()), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        y += 32 - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GOLD);
    }
}

