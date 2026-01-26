package de.mrjulsen.crn.client.gui.widgets.routedetails;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

public class RouteDetailsViewer extends DLGuiComponent {
    
    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;
    
    public final BooleanProperty expanded = new BooleanProperty(false);
    public final BooleanProperty showTrainDetails = new BooleanProperty(true);
    public final BooleanProperty showEntireJourney = new BooleanProperty(false);
    public final BooleanProperty canExpandCollapse = new BooleanProperty(true);

    public RouteDetailsViewer(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, width(), height()));
        contentPanel.anchor.set2(EAlign.values());
        contentPanel.inputConsumptionPolicy.set((type) -> false);
        
        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(10, 9, 10, 0));
        layout.wrap.set(false);
        contentPanel.layout.set(layout);

        scrollbar = addComponent(new DLScrollBar(width() - 5, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
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
    }
    

    public void displayRoute(ClientRoute route) {
        displayRouteInternal(route, route.getClientParts(), true);
    }    

    public void displayPart(ClientRoute route, Predicate<RoutePart> verifiedSelector) {
        displayRouteInternal(route, route.getClientParts().stream().filter(verifiedSelector).toList(), false);
    }

    public void displayRouteInternal(ClientRoute route, List<ClientRoutePart> parts, boolean showTransfers) {
        contentPanel.clearComponents();

        Queue<TransferConnection> connections = new ConcurrentLinkedQueue<>(route.getConnections());
        for (int i = 0; i < parts.size(); i++) {
            ClientRoutePart part = parts.get(i);
            
            RoutePartWidget widget = new RoutePartWidget(width(), route, part);
            widget.expanded.set(expanded.get());
            widget.canExpandCollapse.set(canExpandCollapse.get());
            widget.showTrainDetails.set(showTrainDetails.get());
            widget.showEntireJourney.set(showEntireJourney.get());
            
            contentPanel.addComponent(widget);

            if (!connections.isEmpty() && showTransfers) {
                RouteDetailsTransferWidget transfer = contentPanel.addComponent(new RouteDetailsTransferWidget(connections.poll()));
            }
        }
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
