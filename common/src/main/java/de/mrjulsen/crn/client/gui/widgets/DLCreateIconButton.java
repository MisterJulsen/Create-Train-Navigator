package de.mrjulsen.crn.client.gui.widgets;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.ScreenElement;
import com.simibubi.create.foundation.gui.widget.IconButton;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import net.minecraft.client.gui.GuiGraphics;

public class DLCreateIconButton extends IconButton implements IDragonLibWidget {

    private boolean mouseSelected;

    public DLCreateIconButton(int x, int y, int w, int h, ScreenElement icon) {
        super(x, y, w, h, icon);
    }

    public DLCreateIconButton(int x, int y, ScreenElement icon) {
        super(x, y, icon);
    }

    @Override
	public void renderWidget(@Nonnull GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (visible) {
			isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;

			AllGuiTextures button = !isActive() ? AllGuiTextures.BUTTON_DOWN
				: isMouseSelected() ? AllGuiTextures.BUTTON_HOVER : AllGuiTextures.BUTTON;

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			drawBg(matrixStack, button);
			icon.render(matrixStack, getX() + 1, getY() + 1);
		}
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
        return getX();
    }

    @Override
    public int y() {
        return getY();
    }    

    @Override
    public void set_x(int x) {
        this.setX(x);
    }

    @Override
    public void set_y(int y) {
        this.setY(y);
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
