package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class AbstractNotificationPopup extends WidgetContainer {

    protected final DLScreen screen;
    protected final ColorShade shade;
    protected final Animator animator = addRenderableOnly(new Animator());
    private final Consumer<GuiEventListener> removeWidgetFunc;
    private int xOffset;
    private int yOffset;

    private boolean isClosing = false;

    public AbstractNotificationPopup(DLScreen screen, int x, int y, int width, int height, ColorShade shade, Consumer<GuiEventListener> removeWidgetFunc) {
        super(x, y, width, height);
        this.screen = screen;
        this.shade = shade;
        this.removeWidgetFunc = removeWidgetFunc;
        animator.start(3, null, null, null);
    }

    public void close() {
        isClosing = true;
        animator.start(3, null, null, () -> {
            removeWidgetFunc.accept(this);
        });
    }

    public void closeImmediately() {
        isClosing = true;
        removeWidgetFunc.accept(this);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.poseStack().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        if (animator.isRunning()) {
            if (isClosing) {
                graphics.poseStack().translate(0, animator.getCurrentTicksSmooth() * 2, 0);
                GuiUtils.setTint(1, 1, 1, 1f - animator.getPercentage());
            } else {graphics.poseStack().translate(0, animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0);
                GuiUtils.setTint(1, 1, 1, animator.getPercentage());
            }
        }
        
        renderFlyout(graphics, mouseX, mouseY, partialTicks, GuiAreaDefinition.of(this));
        renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, GuiAreaDefinition.of(this));
        graphics.poseStack().popPose();
    }

    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {        
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    public void renderFlyout(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        CreateDynamicWidgets.renderShadow(graphics, contentArea.getX(), contentArea.getY(), contentArea.getWidth(), contentArea.getHeight());
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, contentArea.getX(), contentArea.getY(), contentArea.getWidth(), contentArea.getHeight(), shade);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return true;
    }    
}
