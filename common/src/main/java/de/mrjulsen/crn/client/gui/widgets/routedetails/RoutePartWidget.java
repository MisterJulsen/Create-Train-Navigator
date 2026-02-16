package de.mrjulsen.crn.client.gui.widgets.routedetails;

import java.util.List;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartEntryWidget.TrainStopType;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

public class RoutePartWidget extends DLGuiComponent {

    private final ClientRoutePart part;
    private final ClientRoute route;

    public final BooleanProperty expanded = new BooleanProperty(false).withAfterPropertyChangedCallback((o, n) -> initGui());
    public final BooleanProperty showTrainDetails = new BooleanProperty(true).withAfterPropertyChangedCallback((o, n) -> initGui());
    public final BooleanProperty showEntireJourney = new BooleanProperty(false).withAfterPropertyChangedCallback((o, n) -> initGui());
    public final BooleanProperty canExpandCollapse = new BooleanProperty(true).withAfterPropertyChangedCallback((o, n) -> initGui());


    public RoutePartWidget(int width, ClientRoute route, ClientRoutePart part) {
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
        boolean valid = route.isPartReachable(part);
        List<ClientTrainStop> stops = showEntireJourney.get() ? part.getAllJourneyClientStops() : part.getAllClientStops();

        addComponent(new RoutePartEntryWidget(part, route, stops.get(0), TrainStopType.START, valid)); 
        if (showTrainDetails.get()) {
            addComponent(new TrainDetailsWidget(this, route, part, stops.get(0)));
            //RoutePartTrainDetailsWidget details = new RoutePartTrainDetailsWidget(this, route, part, stops.get(0), x(), y() + stackLayoutY, width());
            //addToStackLayout(details);
        }
        
        if (this.expanded.get()) {
            for (int i = 1; i < stops.size() - 1; i++) {
                ClientTrainStop stop = stops.get(i);
                addComponent(new RoutePartEntryWidget(part, route, stop, TrainStopType.TRANSIT, valid));
            }
        }  
        addComponent(new RoutePartEntryWidget(part, route, stops.get(stops.size() - 1), TrainStopType.END, valid)); 
    }
}
