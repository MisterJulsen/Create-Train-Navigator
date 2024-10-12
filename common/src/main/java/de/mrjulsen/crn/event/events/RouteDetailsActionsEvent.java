package de.mrjulsen.crn.event.events;

import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.client.gui.widgets.routedetails.RoutePartWidget.RoutePartDetailsActionBuilder;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public class RouteDetailsActionsEvent extends AbstractCRNEvent<RouteDetailsActionsEvent.IRouteDetailsActionsEventData> {

    public Collection<RoutePartDetailsActionBuilder> run(ClientRoute route, ClientRoutePart part, boolean expanded) {
        List<RoutePartDetailsActionBuilder> res = listeners.values().stream().flatMap(x -> x.run(route, part, expanded).stream()).toList();
        tickPost();
        return res;
    }

    @FunctionalInterface
    public static interface IRouteDetailsActionsEventData {
        Collection<RoutePartDetailsActionBuilder> run(ClientRoute route, ClientRoutePart part, boolean expanded);
    }
}
