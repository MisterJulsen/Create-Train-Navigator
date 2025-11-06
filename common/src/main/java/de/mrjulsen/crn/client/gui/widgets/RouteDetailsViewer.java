package de.mrjulsen.crn.client.gui.widgets;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RouteDetailsTransferWidget;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartWidget;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.math.Size;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class RouteDetailsViewer extends DLGuiComponent {

    private final DLScrollBar scrollBar;
    private int contentHeight = 0;
    private Set<RoutePart> expandedParts = new HashSet<>();
    private boolean canExpandCollapse = true;
    private boolean showTrainDetails = true;
    private boolean initialExpanded = false;
    private boolean showJourney = false;

    private final Screen parent;

    public RouteDetailsViewer(Screen parent, int x, int y, int width, int height, DLScrollBar scrollBar) {
        super(x, y, width, height);
        this.scrollBar = scrollBar;
        this.parent = parent;
        scrollBar.scrollerSize.set(0);
        scrollBar.screenSize.set(height());
        scrollBar.maxSize.set(Size.of(width(), 0));
        scrollBar.scrollSteps.set(10);
        scrollBar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            setScrollOffsetY(e.value());
            return false;
        });
    }

    public void displayRoute(ClientRoute route) {
        displayRouteInternal(route, route.getClientParts(), true);
    }    

    public void displayPart(ClientRoute route, Predicate<RoutePart> verifiedSelector) {
        displayRouteInternal(route, route.getClientParts().stream().filter(verifiedSelector).toList(), false);
    }

    public void displayRouteInternal(ClientRoute route, List<ClientRoutePart> parts, boolean displayConnections) {
        clearComponents();
        contentHeight = 10;
        Queue<TransferConnection> connections = new ConcurrentLinkedQueue<>(route.getConnections());
        for (int i = 0; i < parts.size(); i++) {
            ClientRoutePart part = parts.get(i);
            
            RoutePartWidget widget = new RoutePartWidget(parent, x(), y() + contentHeight, width(), route, part);
            widget.setShowTrainDetails(showTrainDetails);
            widget.setCanExpandCollapse(canExpandCollapse);
            widget.setShowJourney(showJourney);
            widget.setExpanded(expandedParts.contains(part) || initialExpanded);
            widget.withOnGuiChangedEvent((w) -> {
                if (w.isExpanded()) expandedParts.add(part); else expandedParts.remove(part);
                displayRoute(route);
            });
            addComponent(widget);
            contentHeight += widget.height();

            if (!connections.isEmpty() && displayConnections) {
                RouteDetailsTransferWidget transfer = addComponent(new RouteDetailsTransferWidget(x(), y() + contentHeight, width(), connections.poll()));
                contentHeight += transfer.height();
            }
        }

        contentHeight += 10;
        scrollBar.maxSize.set(Size.of(width(), contentHeight));
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, 0, 0, 22, 10, 0, 179, 22, 1, TextureFillMode.STRETCH);
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, 0, 0 + contentHeight - 10, 22, Math.max(10, height() - contentHeight + 10), 0, 179, 22, 1, TextureFillMode.STRETCH);

        GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);

    }

    public boolean canExpandCollapse() {
        return canExpandCollapse;
    }

    public void setCanExpandCollapse(boolean canExpandCollapse) {
        this.canExpandCollapse = canExpandCollapse;
    }

    public boolean showTrainDetails() {
        return showTrainDetails;
    }

    public void setShowTrainDetails(boolean showTrainDetails) {
        this.showTrainDetails = showTrainDetails;
    }

    public boolean isInitialExpanded() {
        return initialExpanded;
    }

    public void setInitialExpanded(boolean b) {
        this.initialExpanded = b;
    }

    public boolean isShowingJourney() {
        return showJourney;
    }

    public void setShowJourney(boolean b) {
        this.showJourney = b;
    }    
}
