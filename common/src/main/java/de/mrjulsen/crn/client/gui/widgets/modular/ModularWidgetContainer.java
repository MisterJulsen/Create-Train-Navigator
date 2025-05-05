package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.List;
import java.util.function.BiConsumer;

import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.*;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ModularWidgetContainer extends DLScrollableWidgetContainer {

    public static final int DEFAULT_PADDING = 10;

    private final DLScreen screen;
    private final DLAbstractScrollBar<?> scrollBar;
    private final BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    public ModularWidgetContainer(DLScreen screen, int x, int y, int width, int height, BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder, DLAbstractScrollBar<?> scrollBar) {
        this(screen, x, y, width, height, builder, scrollBar, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
    }

    /*
    TODO: FIXES BUG IN DRAGONLIB! THIS SOLUTION IS VERY UGLY, BUT IT WORKS FOR NOW
     */
    @Override
    public boolean containerMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        mouseY += getYScrollOffset();

        List<? extends GuiEventListener> listeners = childrenLayered();

        for (GuiEventListener listener : listeners) {
            if (listener instanceof IDragonLibContainer container && listener != this && listener.isMouseOver(mouseX, mouseY) && container.containerMouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }

            if (listener instanceof IDragonLibWidget widget && ((listener instanceof IExtendedAreaWidget ext && ext.isInArea(mouseX, mouseY)) || widget.isMouseSelected()) && listener.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }

            if (listener instanceof AbstractWidget widget && widget.isMouseOver(mouseX, mouseY) && widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }

        return consumeScrolling(mouseX, mouseY);
    }

    public ModularWidgetContainer(DLScreen screen, int x, int y, int width, int height, BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder, DLAbstractScrollBar<?> scrollBar, int paddingLeft, int paddingRight, int paddingTop, int paddingBottom) {
        super(x, y, width, height);
        this.screen = screen;
        this.scrollBar = scrollBar;
        this.builder = builder;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        build();
    }

    public void build() {
        clearWidgets();
        ModularWidgetBuilder mb = new ModularWidgetBuilder(this);
        builder.accept(this, mb);
        mb.build();
    }

    int addLine(ModularWidgetLine line, int yOffset) {
        line.set_x(x() + paddingLeft);
        line.set_y(y() + paddingTop + yOffset);
        line.set_width(width() - paddingLeft - paddingRight);
        addRenderableWidget(line);
        return line.height();
    }

    public DLScreen getParentScreen() {
        return screen;
    }

    public DLAbstractScrollBar<?> getScrollbar() {
        return scrollBar;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        if (scrollBar.getScrollValue() > 0) {
            GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        }
        if (scrollBar.getScrollValue() < scrollBar.getMaxScroll()) {
            GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
        }
    }

    @Override
    public void set_width(int w) {
        throw new IllegalStateException("Changing the width is not supported.");
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
    
}
