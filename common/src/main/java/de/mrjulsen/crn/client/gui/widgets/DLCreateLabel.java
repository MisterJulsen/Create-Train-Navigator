package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.widget.Label;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import net.minecraft.network.chat.Component;

public class DLCreateLabel extends Label implements IDragonLibWidget {

    private boolean mouseSelected;

    public DLCreateLabel(int x, int y, Component text) {
        super(x, y, text);
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
