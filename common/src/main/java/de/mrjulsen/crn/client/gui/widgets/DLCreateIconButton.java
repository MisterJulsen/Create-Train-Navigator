package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.element.ScreenElement;
import com.simibubi.create.foundation.gui.widget.IconButton;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;

public class DLCreateIconButton extends IconButton implements IDragonLibWidget {

    private boolean mouseSelected;

    public DLCreateIconButton(int x, int y, int w, int h, ScreenElement icon) {
        super(x, y, w, h, icon);
    }

    public DLCreateIconButton(int x, int y, ScreenElement icon) {
        super(x, y, icon);
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
