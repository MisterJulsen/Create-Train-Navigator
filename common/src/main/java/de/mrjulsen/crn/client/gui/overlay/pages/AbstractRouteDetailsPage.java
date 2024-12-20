package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public abstract class AbstractRouteDetailsPage extends DLRenderable {

    protected final Font font = Minecraft.getInstance().font;
    protected final ClientRoute route;

    public AbstractRouteDetailsPage(ClientRoute route) {
        super(0, 0, 220, 62);
        this.route = route;
    }

    public abstract boolean isImportant();
    
}
