package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.function.BiConsumer;
import java.util.function.Function;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DLOptionsList extends ScrollableWidgetContainer {

    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;

    private final Screen parent;

    public DLOptionsList(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.scrollBar = scrollBar;
        this.parent = parent;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.updateMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public void clearOptions() {
        clearWidgets();
        rearrangeContent();
    }

    public int getContentWidth() {
        return width() - 20;
    }

    public <T extends WidgetContainer> OptionEntry<T> addOption(Function<OptionEntry<T>, T> contentContainer, Component text, Component description, BiConsumer<OptionEntry<T>, OptionEntryHeader> onHeaderClick, Function<String, Boolean> onTitleEdited) {
        OptionEntry<T> entry = new OptionEntry<T>(parent, this, 0, y(), width() - 20, contentContainer, text, description, x -> rearrangeContent(), onHeaderClick, onTitleEdited);
        addRenderableWidget(entry);
        return entry;
    }

    @Override
    public <T extends GuiEventListener & Widget> T addRenderableWidget(T guiEventListener) {
        T t =  super.addRenderableWidget(guiEventListener);
        if (t instanceof IDragonLibWidget wgt) {
            wgt.set_x(x() + 10);
            wgt.set_width(Math.min(width() - 20, wgt.width()));
        } else if (t instanceof AbstractWidget wgt) { 
            wgt.x = x() + 10;
            wgt.setWidth(Math.min(width() - 20, wgt.getWidth()));
        }
        rearrangeContent();
        return t;
    }

    public void rearrangeContent() {
        contentHeight = 10;
        for (GuiEventListener listener : children()) {
            if (listener instanceof OptionEntry<?> wgt) {
                wgt.set_y(y() + contentHeight);
                contentHeight += wgt.height() + (wgt.isExpanded() ? 6 : 3);
            } else if (listener instanceof IDragonLibWidget wgt) {
                wgt.set_y(y() + contentHeight);
                contentHeight += wgt.height() + 3;
            } else if (listener instanceof AbstractWidget wgt) {                
                wgt.y = y() + contentHeight;
                contentHeight += wgt.getHeight() + 3;
            }
        }        
        contentHeight += 10;
        scrollBar.updateMaxScroll(contentHeight);
        if (!scrollBar.canScroll()) {
            scrollBar.scrollTo(0);
        }
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
        if (children().isEmpty()) {
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".empty_list"), DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED, EAlignment.CENTER, false);
        }
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
