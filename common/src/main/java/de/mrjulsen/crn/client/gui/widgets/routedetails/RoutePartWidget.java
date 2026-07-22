package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartEntryWidget.TrainStopType;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

public class RoutePartWidget extends DLGuiComponent {

    private final RouteLeg part;
    private final RouteJourney route;

    public final BooleanProperty expanded = new BooleanProperty(false).withAfterPropertyChangedCallback((o, n) -> initGui());
    public final BooleanProperty showTrainDetails = new BooleanProperty(true).withAfterPropertyChangedCallback((o, n) -> initGui());
    public final BooleanProperty canExpandCollapse = new BooleanProperty(true).withAfterPropertyChangedCallback((o, n) -> initGui());


    public RoutePartWidget(int width, RouteJourney route, RouteLeg part) {
        super(0, 0, width, 1);
        this.part = part;
        this.route = route;
        
        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.wrap.set(false);
        this.layout.set(layout);

        addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {
            this.setHeight(e.layoutResult().contentHeight());
            return false;
        });

    }

    public void initGui() {
        clearComponents();
        boolean valid = route.isLegReachable(part);

        addComponent(new RoutePartEntryWidget(part, route, part.boarding(), TrainStopType.START, valid));
        if (showTrainDetails.get()) {
            addComponent(new TrainDetailsWidget(this, part));
        }

        if (this.expanded.get()) {
            for (RouteCall call : part.intermediateCalls()) {
                addComponent(new RoutePartEntryWidget(part, route, call, TrainStopType.TRANSIT, valid));
            }
        }
        addComponent(new RoutePartEntryWidget(part, route, part.alighting(), TrainStopType.END, valid));
    }
}
