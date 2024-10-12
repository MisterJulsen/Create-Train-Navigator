package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;

import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class RouteViewer extends ScrollableWidgetContainer {

    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;

    public RouteViewer(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.updateMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public Screen getParent() {
        return parent;
    }

    public void displayRoutes(List<ClientRoute> routes) {
        clearWidgets();
        contentHeight = 5;
        for (int i = 0; i < routes.size(); i++) {
            RouteWidget widget = new RouteWidget(this, routes.get(i), x + 10, y + contentHeight);
            addRenderableWidget(widget);
            contentHeight += (RouteWidget.HEIGHT + 3);
        }
        contentHeight += 2;
        scrollBar.updateMaxScroll(contentHeight);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        
        GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
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

    public void clear() {
        clearWidgets();
    }
    
}
