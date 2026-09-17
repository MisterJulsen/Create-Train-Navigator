package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public abstract class AbstractRouteDetailsPage extends DLGuiComponent {

    protected final Font font = Minecraft.getInstance().font;
    protected final JourneyTracker tracker;

    public AbstractRouteDetailsPage(JourneyTracker tracker) {
        super(0, 0, 220, 62);
        this.tracker = tracker;
    }

    protected RouteJourney route() {
        return tracker.journey();
    }

    public abstract boolean isImportant();

}
