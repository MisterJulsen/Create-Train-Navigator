package de.mrjulsen.crn.client.gui.screen;

import java.util.UUID;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.RouteDetailsViewer;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

public class TrainJourneySreen extends AbstractNavigatorScreen {

    private final ClientRoute route;
    private final ClientRoutePart part;

    private RouteDetailsViewer viewer;

    public TrainJourneySreen(Screen lastScreen, ClientRoute route, UUID trainId) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.title"), BarColor.GOLD);
        this.route = route;
        this.part = route.getClientParts().stream().filter(x -> x.getTrainId().equals(trainId)).findFirst().orElse(route.getFirstClientPart());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        int dy = FooterSize.DEFAULT.size() + 32;
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, guiLeft + GUI_WIDTH - 8, guiTop + dy, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, null);
        viewer = new RouteDetailsViewer(this, guiLeft + 3, guiTop + dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, scrollBar);
        viewer.setShowTrainDetails(false);
        viewer.setCanExpandCollapse(false);
        viewer.setInitialExpanded(true);
        viewer.setShowJourney(true);
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);
        viewer.displayPart(route, x -> x == part);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderNavigatorBackground(graphics, mouseX, mouseY, partialTicks);        
        
        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, 32, ContainerColor.BLUE);
        GuiUtils.drawString(graphics, font, guiLeft + 8, guiTop + y + 7, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.date", (ModCommonEvents.getPhysicalLevel().dayTime() / Level.TICKS_PER_DAY)), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, guiLeft + 8, guiTop + y + 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.train", part.getFirstStop().getTrainName(), part.getFirstStop().getTrainId().toString().split("-")[0], part.getFirstStop().getDisplayTitle()), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
        y += 32 - 1;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GOLD);

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

    }
}
