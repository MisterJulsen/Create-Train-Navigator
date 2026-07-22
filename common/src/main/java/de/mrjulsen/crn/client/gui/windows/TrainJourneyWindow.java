package de.mrjulsen.crn.client.gui.windows;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RouteDetailsViewer;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.network.packets.pain.GetTrainRealtimePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.client.Minecraft;

public class TrainJourneyWindow extends AbstractNavigatorScreen {

    private final UUID trainId;
    private final RouteLeg ridden;
    private final RouteDetailsViewer viewer;

    private Optional<RouteLeg> journey = Optional.empty();

    /**
     * The full run of the service the given leg is part of. The leg says which section to show and
     * which cycle of it the traveller is on, so the run shown is the one they are actually riding.
     */
    public TrainJourneyWindow(DLWindowManager manager, RouteLeg ridden) {
        this(manager, ridden.trainId(), ridden);
    }

    /** The full run of the section the train is working through right now. */
    public TrainJourneyWindow(DLWindowManager manager, UUID trainId) {
        this(manager, trainId, null);
    }

    private TrainJourneyWindow(DLWindowManager manager, UUID trainId, RouteLeg ridden) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.title"), ContainerColor.GRAY, BarColor.GOLD);
        this.trainId = trainId;
        this.ridden = ridden;

        int dy = FooterSize.DEFAULT.size() + 32;
        viewer = addComponent(new RouteDetailsViewer(3, dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1));
        viewer.showTrainDetails.set(false);
        viewer.canExpandCollapse.set(false);
        viewer.expanded.set(true);

        requestJourney();
    }

    /**
     * Fetches the train's journey and shows one section of it end to end. Not the whole schedule:
     * what a traveller wants to see is the service they are on, from where it starts out to where it
     * terminates, and the rest of the train's day is a different service that happens to use the
     * same carriages.
     */
    private void requestJourney() {
        ModNetworkManager.GET_TRAIN_REALTIME.send(NetworkDirection.toServer(), new GetTrainRealtimePacketData.Request(trainId, true), (response) -> {
            response.getTrain().ifPresent(train -> response.getJourney().ifPresent(snapshot -> {
                RouteLeg leg = ridden == null
                    ? RouteLeg.ofCurrentSection(train, snapshot)
                    : RouteLeg.ofSection(train, snapshot, ridden.sectionIndex(), ridden.boarding());
                journey = Optional.of(leg);
                viewer.displayRoute(new RouteJourney(List.of(leg), List.of()));
            }));
        }, () -> {});
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);

        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, 32, ContainerColor.BLUE);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 8, y + 7, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.date", (long)DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem()).toGameDays()), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        final int headerY = y;
        journey.ifPresent(leg -> GuiUtils.drawString(graphics, graphics.defaultFont(), 8, headerY + 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.train", leg.displayName(), leg.trainId().toString().split("-")[0], leg.destinationText()), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false));
        y += 32 - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GOLD);
    }
}
