package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class AbstractFlyoutWidget<T extends GuiEventListener & Renderable & NarratableEntry> extends WidgetContainer {

    protected final DLScreen screen;
    protected final FlyoutPointer pointer;
    protected final ColorShade pointerShade;
    protected final int distanceToParent = 0;
    protected final Animator animator = addRenderableOnly(new Animator());
    private final Consumer<T> addRenderableWidgetFunc;
    private final Consumer<GuiEventListener> removeWidgetFunc;
    private int xOffset;
    private int yOffset;

    private boolean isClosing = false;

    private final Cache<GuiAreaDefinition> contentArea = new Cache<>(() -> new GuiAreaDefinition(x() + (FlyoutPointer.WIDTH - 2), y() + (FlyoutPointer.HEIGHT - 2), width() - (FlyoutPointer.WIDTH - 2) * 2, height() - (FlyoutPointer.HEIGHT - 2) * 2));

    public AbstractFlyoutWidget(DLScreen screen, int width, int height, FlyoutPointer pointer, ColorShade pointerShade, Consumer<T> addRenderableWidgetFunc, Consumer<GuiEventListener> removeWidgetFunc) {
        super(0, 0, width, height);
        this.screen = screen;
        this.pointerShade = pointerShade;
        this.pointer = pointer;
        this.addRenderableWidgetFunc = addRenderableWidgetFunc;
        this.removeWidgetFunc = removeWidgetFunc;
    }

    public GuiAreaDefinition getContentArea() {
        return contentArea.get();
    }

    @SuppressWarnings({ "resource", "unchecked" })
    public void open(IDragonLibWidget parent) {
        screen.setAllowedLayer(screen.getAllowedLayer() + 1);
        setWidgetLayerIndex(screen.getAllowedLayer());

        switch (pointer) {
            case UP -> {
                set_x(MathUtils.clamp(xOffset + parent.x() + parent.width() / 2 - width() / 2, 0, Minecraft.getInstance().screen.width - width()));
                set_y(MathUtils.clamp(yOffset + parent.y() + parent.height() + distanceToParent, 0, Minecraft.getInstance().screen.height - height() - distanceToParent));
            }
            case DOWN -> {
                set_x(MathUtils.clamp(xOffset + parent.x() + parent.width() / 2 - width() / 2, 0, Minecraft.getInstance().screen.width - width()));
                set_y(MathUtils.clamp(yOffset + parent.y() - height() - distanceToParent, 0, Minecraft.getInstance().screen.height - height() - distanceToParent));
            }
            case RIGHT -> {
                set_x(MathUtils.clamp(xOffset + parent.x() - width() - distanceToParent, 0, Minecraft.getInstance().screen.width - width() - distanceToParent));
                set_y(MathUtils.clamp(yOffset + parent.y() + parent.height() / 2 - height() / 2, 0, Minecraft.getInstance().screen.height - height()));
            }
            case LEFT -> {
                set_x(MathUtils.clamp(xOffset + parent.x() + parent.width() + distanceToParent, 0, Minecraft.getInstance().screen.width - width() - distanceToParent));
                set_y(MathUtils.clamp(yOffset + parent.y() + parent.height() / 2 - height() / 2, 0, Minecraft.getInstance().screen.height - height()));
            }
        }
        contentArea.clear();
        animator.start(3, null, null, null);
        addRenderableWidgetFunc.accept((T)this);
    }

    public void close() {
        isClosing = true;
        screen.setAllowedLayer(getWidgetLayerIndex() - 1);
        animator.start(3, null, null, () -> {
            screen.setAllowedLayer(getWidgetLayerIndex() - 1);
            removeWidgetFunc.accept(this);
        });
    }

    public void closeImmediately() {
        isClosing = true;
        screen.setAllowedLayer(getWidgetLayerIndex() - 1);
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
                switch (pointer) {
                    case UP -> graphics.poseStack().translate(0, -animator.getCurrentTicksSmooth() * 2, 0);
                    case DOWN -> graphics.poseStack().translate(0, animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0);
                    case LEFT -> graphics.poseStack().translate(animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0, 0);
                    case RIGHT -> graphics.poseStack().translate(animator.getCurrentTicksSmooth() * 2, 0, 0);
                }
                GuiUtils.setTint(1, 1, 1, 1f - animator.getPercentage());
            } else {
                switch (pointer) {
                    case UP -> graphics.poseStack().translate(0, -animator.getTotalTicks() * 2 + animator.getCurrentTicksSmooth() * 2, 0);
                    case DOWN -> graphics.poseStack().translate(0, animator.getCurrentTicksSmooth() * 2, 0);
                    case LEFT -> graphics.poseStack().translate(-animator.getCurrentTicksSmooth() * 2, 0, 0);
                    case RIGHT -> graphics.poseStack().translate(animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0, 0);
                }
                GuiUtils.setTint(1, 1, 1, animator.getPercentage());
            }
        }
        
        renderFlyout(graphics, mouseX, mouseY, partialTicks, getContentArea());
        renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, getContentArea());
        graphics.poseStack().popPose();
    }

    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {        
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    public void renderFlyout(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        CreateDynamicWidgets.renderShadow(graphics, contentArea.getX(), contentArea.getY(), contentArea.getWidth(), contentArea.getHeight());
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, contentArea.getX(), contentArea.getY(), contentArea.getWidth(), contentArea.getHeight(), pointerShade);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        switch (pointer) {
            case UP:
                pointer.render(graphics, x() + width() / 2 - FlyoutPointer.WIDTH / 2, y(), pointerShade);
                break;
            case DOWN:
                pointer.render(graphics, x() + width() / 2 - FlyoutPointer.WIDTH / 2, y() + height() - FlyoutPointer.HEIGHT, pointerShade);
                break;
            case LEFT:
                pointer.render(graphics, x(), y() + height() / 2 - FlyoutPointer.HEIGHT / 2, pointerShade);
                break;
            case RIGHT:
                pointer.render(graphics, x() + width() - FlyoutPointer.WIDTH, y() + height() / 2 - FlyoutPointer.HEIGHT / 2, pointerShade);
                break;
            default:
                break;
        }
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
    
    public static enum FlyoutPointer {
        UP(0, 54),
        DOWN(14, 54),
        RIGHT(28, 54),
        LEFT(42, 54);

        private final int u;
        private final int v;
        public static final int WIDTH = 7;
        public static final int HEIGHT = 7;

        private FlyoutPointer(int u, int v) {
            this.u = u;
            this.v = v;
        }

        public int getU() {
            return u;
        }

        public int getV() {
            return v;
        }

        public void render(Graphics graphics, int x, int y, ColorShade shade) {
            int u = shade == ColorShade.LIGHT ? getU() : getU() + WIDTH;
            GuiUtils.drawTexture(CRNGui.GUI, graphics, x, y, WIDTH, HEIGHT, u, v, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT);
        }
    }
}
