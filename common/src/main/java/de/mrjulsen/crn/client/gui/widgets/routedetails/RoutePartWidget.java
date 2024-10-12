package de.mrjulsen.crn.client.gui.widgets.routedetails;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartEntryWidget.TrainStopType;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.DLWidgetsCollection;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RoutePartWidget extends WidgetContainer {

    private final ClientRoutePart part;
    private final ClientRoute route;
    private int stackLayoutY;

    private boolean expanded;
    private boolean canExpandCollapse = true;
    private boolean showTrainDetails = true;
    private boolean showJourney = false;
    private Consumer<RoutePartWidget> onGuiChanged;
    private final DLWidgetsCollection stationWidgets = new DLWidgetsCollection();

    private final Screen parent;

    public static final int ACTION_BTN_WIDTH = 140;
    public static final int ACTION_BTN_HEIGHT = 14;

    public RoutePartWidget(Screen parent, int x, int y, int width, ClientRoute route, ClientRoutePart part) {
        super(x, y, width, 1);
        this.part = part;
        this.route = route;
        this.parent = parent;
        initGui();
    }

    public void initGui() {
        children().stream().filter(x -> x instanceof Closeable).forEach(x -> {
            try {
                ((Closeable)x).close();
            } catch (IOException e) {}
        });
        clearWidgets();
        stackLayoutY = 0;
        stationWidgets.clear();
        boolean valid = route.isPartReachable(part);

        List<ClientTrainStop> stops = showJourney ? part.getAllJourneyClientStops() : part.getAllClientStops();

        addToStackLayout(new RoutePartEntryWidget(parent, part, stops.get(0), x(), y() + stackLayoutY, width(), TrainStopType.START, valid)); 
        if (showTrainDetails()) {
            RoutePartTrainDetailsWidget details = new RoutePartTrainDetailsWidget(parent, this, route, part, stops.get(0), x(), y() + stackLayoutY, width());
            addToStackLayout(details);
        }
        
        if (this.expanded) {
            for (int i = 1; i < stops.size() - 1; i++) {
                ClientTrainStop stop = stops.get(i);
                addToStackLayout(new RoutePartEntryWidget(parent, part, stop, x(), y() + stackLayoutY, width(), TrainStopType.TRANSIT, valid));
            }
        }  
        addToStackLayout(new RoutePartEntryWidget(parent, part, stops.get(stops.size() - 1), x(), y() + stackLayoutY, width(), TrainStopType.END, valid)); 

        set_height(stackLayoutY);

        DLUtils.doIfNotNull(onGuiChanged, x -> x.accept(this));
    }

    public RoutePartWidget withOnGuiChangedEvent(Consumer<RoutePartWidget> onGuiChanged) {
        this.onGuiChanged = onGuiChanged;
        return this;
    }

    public void updateHeight() {
        stackLayoutY = 0;
        for (IDragonLibWidget c : stationWidgets.components) {
            c.set_y(y() + stackLayoutY);
            stackLayoutY += c.height();
        }
    }


    private <T extends GuiEventListener & Renderable & IDragonLibWidget> void addToStackLayout(T widget) {
        addRenderableWidget(widget);
        stationWidgets.add(widget);
        stackLayoutY += widget.height();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
        initGui();
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

    public boolean isShowingJourney() {
        return showJourney;
    }

    public void setShowJourney(boolean showJourney) {
        this.showJourney = showJourney;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        //GuiUtils.drawString(graphics, font, x() + 22, y(), "State: " + part.getProgressState() + ", " + part.getNextStop().getTag().getTagName().get(), 0xFFFF0000, EAlignment.LEFT, false);
    }

    @Override
    public void tick() {
        super.tick();
        boolean valid = route.isPartReachable(part);
        stationWidgets.performForEach(x -> x instanceof RoutePartEntryWidget, x -> ((RoutePartEntryWidget)x).setValid(valid));
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

    public static record RoutePartDetailsActionBuilder(Component text, Sprite icon, Consumer<DLIconButton> onClick) {}    
}
