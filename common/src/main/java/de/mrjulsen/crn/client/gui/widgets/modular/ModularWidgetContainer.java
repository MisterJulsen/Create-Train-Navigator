package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.function.BiConsumer;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class ModularWidgetContainer extends DLGuiComponent {

    public static final int DEFAULT_PADDING = 10;

    private final DLScrollBar scrollBar;
    private final BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    public ModularWidgetContainer(int x, int y, int width, int height, BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder, DLScrollBar scrollBar) {
        this(x, y, width, height, builder, scrollBar, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
    }

    public ModularWidgetContainer(int x, int y, int width, int height, BiConsumer<ModularWidgetContainer, ModularWidgetBuilder> builder, DLScrollBar scrollBar, int paddingLeft, int paddingRight, int paddingTop, int paddingBottom) {
        super(x, y, width, height);
        this.scrollBar = scrollBar;
        this.builder = builder;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        build();
    }

    public void build() {
        clearComponents();
        ModularWidgetBuilder mb = new ModularWidgetBuilder(this);
        builder.accept(this, mb);
        mb.build();
    }

    int addLine(ModularWidgetLine line, int yOffset) {
        return 0;
    }

    public DLScrollBar getScrollbar() {
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
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (scrollBar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
        if (scrollBar.value.get() < scrollBar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);
        }
    }

    @Override
    public void setWidth(double width) {
        throw new IllegalStateException("Changing the width is not supported.");
    }
    
}
