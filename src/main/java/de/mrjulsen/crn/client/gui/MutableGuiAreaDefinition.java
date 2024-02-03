package de.mrjulsen.crn.client.gui;

import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;

public class MutableGuiAreaDefinition extends GuiAreaDefinition {
    private int xOffset, yOffset;

    public MutableGuiAreaDefinition(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public void setXOffset(int x) {
        this.xOffset = x;
    }

    public void setYOffset(int y) {
        this.yOffset = y;
    }

    public int getXOffset() {
        return this.xOffset;
    }

    public int getYOffset() {
        return this.yOffset;
    }

    @Override
    public int getRight() {
        return super.getRight() + getXOffset();
    }

    @Override
    public int getBottom() {
        return super.getBottom() + getYOffset();
    }

    @Override
    public int getX() {
        return super.getX() + getXOffset();
    }

    @Override
    public int getY() {
        return super.getY() + getYOffset();
    }

    public boolean isInBounds(double mouseX, double mouseY) {
        return mouseX >= getLeft() && mouseX < getRight() && mouseY >= getTop() && mouseY < getBottom();
    }
}

