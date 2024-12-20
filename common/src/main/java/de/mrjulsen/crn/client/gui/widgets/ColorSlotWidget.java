package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutColorPicker;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.Component;

public class ColorSlotWidget extends DLButton {

    public static final int SLOT_SIZE = 18;

    private final DLScreen parent;
    private final List<Component> tooltip;
    private final Supplier<Integer> getScrollOffset;
    private final Consumer<ColorSlotWidget> onColorSelected;
    private final int[] defaultColors;
    private final boolean allowCustom;
    private final boolean allowNone;
    private int selectedColor;
    
    public ColorSlotWidget(DLScreen parent, int x, int y, int color, int[] defaultColors, boolean allowCustom, boolean allowNone, List<Component> tooltip, Supplier<Integer> getScrollOffset, Consumer<ColorSlotWidget> onColorSelected) {
        super(x, y, SLOT_SIZE, SLOT_SIZE, TextUtils.empty());
        this.parent = parent;
        this.getScrollOffset = getScrollOffset;
        this.defaultColors = defaultColors;
        this.allowCustom = allowCustom;
        this.allowNone = allowNone;
        this.onColorSelected = onColorSelected;
        this.tooltip = tooltip;
        this.selectedColor = color;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        FlyoutColorPicker<?> flyout = new FlyoutColorPicker<>(parent, selectedColor, defaultColors, 8, allowCustom, allowNone,
        (w) -> {
            renderTooltip = false;
            parent.addRenderableWidget(w);
        },
        (w) -> {
            renderTooltip = true;
            this.selectedColor = ((FlyoutColorPicker<?>)w).getColorPicker().getSelectedColor();
            DLUtils.doIfNotNull(onColorSelected, x -> x.accept(this));
            parent.removeWidget(w);
        });
        flyout.setYOffset((int)-getScrollOffset.get());
        flyout.open(this);
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        DynamicGuiRenderer.renderArea(graphics, x(), y(), width(), height(), AreaStyle.GRAY, ButtonState.DOWN);
        GuiUtils.fill(graphics, x() + 1, y() + 1, width() - 2, height() - 2, selectedColor);
        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x() + 1, y() + 1, width() - 2, height() - 2, 0x40FFFFFF);
        }
    }

    private boolean renderTooltip = true;
    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isMouseSelected() || !renderTooltip) {
            return;
        }

        GuiUtils.renderTooltip(parent, this, tooltip, parent.width() / 3, graphics, mouseX, mouseY);
    }
}
