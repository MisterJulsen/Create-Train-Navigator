package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class TrainDebugViewer extends ScrollableWidgetContainer {

    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;

    public TrainDebugViewer(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
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

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
    }

    public void reload() {
        clearWidgets();
        contentHeight = 0;
                
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_TRAINS_DEBUG_DATA, (debug) -> {
            for (int i = 0; i < debug.size(); i++) {
                TrainDebugData debugData = debug.get(i);
                TrainDebugWidget widget = new TrainDebugWidget(parent, this, x() + 10, y() + 5 + contentHeight, width() - 20, debugData);
                addRenderableWidget(widget);
                contentHeight += (widget.height() + 3);
            }
            contentHeight += 7;
            scrollBar.updateMaxScroll(contentHeight);
        });
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

        if (children().isEmpty()) {
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".empty_list"), 0xFFDBDBDB, EAlignment.CENTER, false);
        }
        
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
}
