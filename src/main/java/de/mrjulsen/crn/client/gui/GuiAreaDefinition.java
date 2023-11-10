package de.mrjulsen.crn.client.gui;

public class GuiAreaDefinition {
    private final int x, y, w, h;

    public GuiAreaDefinition(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int getRight() {
        return x + w;
    }

    public int getBottom() {
        return y + h;
    }

    public int getLeft() {
        return this.getX();
    }

    public int getTop() {
        return this.getY();
    }

    public boolean isInBounds(double mouseX, double mouseY) {
        return mouseX >= getLeft() && mouseX < getRight() && mouseY >= getTop() && mouseY < getBottom();
    }

    public static GuiAreaDefinition empty() {
        return new GuiAreaDefinition(0, 0, 0, 0);
    }
}

