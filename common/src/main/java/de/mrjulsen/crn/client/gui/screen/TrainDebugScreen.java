package de.mrjulsen.crn.client.gui.screen;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.TrainDebugViewer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class TrainDebugScreen extends AbstractNavigatorScreen {

    private TrainDebugViewer viewer;

    private GuiAreaDefinition workingArea;

    public TrainDebugScreen(Screen lastScreen) {
        super(lastScreen, TextUtils.text("Train Status Viewer"), BarColor.PURPLE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        int wY = FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - FooterSize.SMALL.size();
        workingArea = new GuiAreaDefinition(guiLeft + 3, guiTop + wY + 2, GUI_WIDTH - 6, wH - 3);
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, workingArea.getRight() - 5, workingArea.getY(), workingArea.getHeight(), null);
        this.viewer = new TrainDebugViewer(this, workingArea.getX(), workingArea.getY(), workingArea.getWidth(), workingArea.getHeight(), scrollBar);
        this.viewer.reload();
        
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);

        this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 18 - 8, guiTop + 223, 18, 18, ModGuiIcons.REFRESH.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                viewer.reload();
            }
        });
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderNavigatorBackground(graphics, mouseX, mouseY, partialTicks);
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX() - 2, workingArea.getY() - 2, workingArea.getWidth() + 4, workingArea.getHeight() + 4, ContainerColor.PURPLE);

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }
}
