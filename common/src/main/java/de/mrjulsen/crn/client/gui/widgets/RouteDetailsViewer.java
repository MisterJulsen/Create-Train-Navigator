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
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class RouteDetailsViewer extends ScrollableWidgetContainer {

    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;
    private Set<RoutePart> expandedParts = new HashSet<>();
    private boolean canExpandCollapse = true;
    private boolean showTrainDetails = true;
    private boolean initialExpanded = false;
    private boolean showJourney = false;

    private final Screen parent;

    public RouteDetailsViewer(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.scrollBar = scrollBar;
        this.parent = parent;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.updateMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public void displayRoute(ClientRoute route) {
        displayRouteInternal(route, route.getClientParts(), true);
    }    

    public void displayPart(ClientRoute route, Predicate<RoutePart> verifiedSelector) {
        displayRouteInternal(route, route.getClientParts().stream().filter(verifiedSelector).toList(), false);
    }

    public void displayRouteInternal(ClientRoute route, List<ClientRoutePart> parts, boolean displayConnections) {
        clearWidgets();
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
            addRenderableWidget(widget);
            contentHeight += widget.height();

            if (!connections.isEmpty() && displayConnections) {
                RouteDetailsTransferWidget transfer = addRenderableOnly(new RouteDetailsTransferWidget(x(), y() + contentHeight, width(), connections.poll()));
                contentHeight += transfer.height();
            }
        }

        contentHeight += 10;
        scrollBar.updateMaxScroll(contentHeight);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);        

        GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);

        //DLUtils.doIfNotNull(route, r -> GuiUtils.drawString(graphics, font, x(), y(), r.getState().name() + ", Running: " + !r.isClosed(), 0xFFFF0000, EAlignment.LEFT, false));
    }
    
    @Override
    public void renderMainLayerScrolled(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x(), y(), 22, 10, 0, 179, 22, 1, 256, 256);
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x(), y() + contentHeight - 10, 22, Math.max(10, height() - contentHeight + 10), 0, 179, 22, 1, 256, 256);
        super.renderMainLayerScrolled(graphics, mouseX, mouseY, partialTicks);
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

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
    
}
