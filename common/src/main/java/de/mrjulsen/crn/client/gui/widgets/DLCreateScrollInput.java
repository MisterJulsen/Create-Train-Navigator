package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class DLCreateScrollInput extends ScrollInput implements IDragonLibWidget {

    private boolean mouseSelected;
    protected boolean renderArrow;
    protected Function<DLCreateScrollInput, List<Component>> onTooltip;

    protected final Cache<List<Component>> cachedTooltip = new Cache<>(() -> {
        List<Component> tooltips = new ArrayList<>(getToolTip());
        DLUtils.doIfNotNull(onTooltip, x -> tooltips.addAll(onTooltip.apply(this)));
        return tooltips;
    });

    protected final DLScreen parent;

    public DLCreateScrollInput(DLScreen parent, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.parent = parent;;
    }

    public DLCreateScrollInput setRenderArrow(boolean b) {
        this.renderArrow = b;
        return this;
    }

    public DLCreateScrollInput onRenderTooltip(Function<DLCreateScrollInput, List<Component>> action) {
        this.onTooltip = action;
        cachedTooltip.clear();
        return this;
    }

    public boolean shouldRenderArrow() {
        return renderArrow;
    }

    public Component getText() {
        return formatter.apply(state);
    }

    @Override
    protected void updateTooltip() {
        super.updateTooltip();
        cachedTooltip.clear();
    }

    @Override
    public final void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        renderMainLayer(new Graphics(ms), mouseX, mouseY, partialTicks);
    }

    @SuppressWarnings("resource")
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        CreateDynamicWidgets.renderTextBox(graphics, x(), y(), width());
        if (shouldRenderArrow()) CreateDynamicWidgets.renderTextBoxArrow(graphics, x(), y());
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, x() + 5, y() + 5, getText(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, true);

    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isMouseSelected()) {
            return;
        }
        GuiUtils.renderTooltip(parent, this, cachedTooltip.get(), parent.width() / 3, graphics, mouseX, mouseY);
    }

    @Override
    public void onFocusChangeEvent(boolean focus) {}

    @Override
    public DLContextMenu getContextMenu() {
        return null;
    }

    @Override
    public void setMenu(DLContextMenu menu) {}

    @Override
    public boolean isMouseSelected() {
        return mouseSelected;
    }

    @Override
    public void setMouseSelected(boolean selected) {
        this.mouseSelected = selected;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }    

    @Override
    public void set_x(int x) {
        this.x = x;
    }

    @Override
    public void set_y(int y) {
        this.y = y;
    }

    @Override
    public void set_width(int w) {
        this.width = w;
    }

    @Override
    public void set_height(int h) {
        this.height = h;
    }

    @Override
    public void set_visible(boolean b) {
        this.visible = b;
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public void set_active(boolean b) {
        this.active = b;
    }

    @Override
    public boolean active() {
        return super.isActive();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }
}
