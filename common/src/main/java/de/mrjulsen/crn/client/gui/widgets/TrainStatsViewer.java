package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.crn.network.packets.pain.GetDepartureAndArrivalRoutesAtPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.EDepartureBoardTrainFilter;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.BorderLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;
import net.minecraft.client.Minecraft;

import java.util.List;

public class TrainStatsViewer extends DLGuiComponent {

    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;

    public TrainStatsViewer(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, 1, 1));
        contentPanel.layoutContraint.set(BorderLayout.BorderPosition.CENTER);
        contentPanel.inputConsumptionPolicy.set((type) -> false);

        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(10, 10, 10, 10));
        layout.wrap.set(false);
        layout.verticalGap.set(3);
        contentPanel.layout.set(layout);

        scrollbar = addComponent(new DLScrollBar(0, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.layoutContraint.set(BorderLayout.BorderPosition.EAST);
        scrollbar.scrollerSize.set(0);
        scrollbar.screenSize.set(contentPanel.height());
        scrollbar.scrollSteps.set(15);
        scrollbar.max.set(0);
        scrollbar.inputConsumptionPolicy.set((type) -> true);
        scrollbar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            contentPanel.setScrollOffsetY(e.value());
            return false;
        });
        
        addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollbar::invokeEvent);

        contentPanel.addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {            
            scrollbar.max.set(e.layoutResult().contentHeight());
            scrollbar.screenSize.set(contentPanel.height());
            return false;
        });

        BorderLayout borderLayout = new BorderLayout(0, 0);
        this.layout.set(borderLayout);
    }

    public void displayTrains() {
        contentPanel.clearComponents();
        contentPanel.suspendLayout();
        ModNetworkManager.GET_ALL_TRAINS_DEBUG_DATA.send(NetworkDirection.toServer(), (response) -> {
            contentPanel.addComponent(new TrainStatsSummary(response.getData().size()));
            for (TrainDebugData data : response.getData()) {
                contentPanel.addComponent(new TrainStatsEntry(data));
            }
        }, () -> {});
        contentPanel.resumeLayout();
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (scrollbar.canScroll() && scrollbar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
        if (scrollbar.canScroll() && scrollbar.value.get() < scrollbar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);
        }
    }
}
