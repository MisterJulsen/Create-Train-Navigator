package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.util.Graphics;

public interface IEntryListSettingsOption {
    void setYPos(int y);
    void tick();
    int calcHeight();
    void renderSuggestions(Graphics graphics, int mouseX, int mouseY, float partialTicks);
    /**
     * This method is always called, even if the used clicked outside the working area of the container window.
     * This additional method should fix the usage of the suggestions popup, which can be rendered outside of the working area.
     * @param pMouseX
     * @param pMouseY
     * @param pButton
     * @return
     */
    boolean mouseClickedLoop(double pMouseX, double pMouseY, int pButton);

    /**
     * This method is always called, even if the used scrolled outside the working area of the container window.
     * This additional method should fix the usage of the suggestions popup, which can be rendered outside of the working area.
     * @param pMouseX
     * @param pMouseY
     * @param pDelta
     * @return
     */
    boolean mouseScrolledLoop(double pMouseX, double pMouseY, double pDelta);
}
