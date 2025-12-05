package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.RenderLayer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public abstract class AbstractFlyoutWidget extends DLWindow {

    protected final DLGuiComponent parentComponent;
    protected final FlyoutPointer pointer;
    protected final ColorShade pointerShade;
    protected final int distanceToParent = 0;
    protected final Animator animator;
    private int xOffset;
    private int yOffset;

    private boolean isClosing = false;
    private boolean isClosed = false;

    protected final Cache<Rectangle> contentArea = new Cache<>(() -> Rectangle.withSize((FlyoutPointer.WIDTH - 2), (FlyoutPointer.HEIGHT - 2), width() - (FlyoutPointer.WIDTH - 2) * 2, height() - (FlyoutPointer.HEIGHT - 2) * 2));

    public AbstractFlyoutWidget(DLWindowManager manager, DLGuiComponent parentComponent, int width, int height, FlyoutPointer pointer, ColorShade pointerShade) {
        super(manager);
        setSize(width, height);
        this.parentComponent = parentComponent;
        this.pointerShade = pointerShade;
        this.pointer = pointer;

        this.animator = addComponent(new Animator());
        
        addEventListener(DLWindow.WindowFocusEvent.class, (src, e) -> {
            if (!e.focus()) {
                for (DLWindow win : getWindowManager().getWindows(this.getAssignedModal().get())) {
                    if (getWindowManager().getFocusedWindow() == win)
                        return false;
                }
                close();
            }
            return false;
        });

        addEventListener(DLWindow.WindowCreatedEvent.class, (s, e) -> {
            enabled.set(false);
            onOpen();
            Point componentPos = parentComponent.toScreenCoordinates();
            switch (pointer) {
                case UP -> {
                    setX(MathUtils.clamp(xOffset + (int)componentPos.x() + parentComponent.width() / 2 - width() / 2, 0, Minecraft.getInstance().screen.width - width()));
                    setY(MathUtils.clamp(yOffset + (int)componentPos.y() + parentComponent.height() + distanceToParent, 0, Minecraft.getInstance().screen.height - height() - distanceToParent));
                }
                case DOWN -> {
                    setX(MathUtils.clamp(xOffset + (int)componentPos.x() + parentComponent.width() / 2 - width() / 2, 0, Minecraft.getInstance().screen.width - width()));
                    setY(MathUtils.clamp(yOffset + (int)componentPos.y() - height() - distanceToParent, 0, Minecraft.getInstance().screen.height - height() - distanceToParent));
                }
                case RIGHT -> {
                    setX(MathUtils.clamp(xOffset + (int)componentPos.x() - width() - distanceToParent, 0, Minecraft.getInstance().screen.width - width() - distanceToParent));
                    setY(MathUtils.clamp(yOffset + (int)componentPos.y() + parentComponent.height() / 2 - height() / 2, 0, Minecraft.getInstance().screen.height - height()));
                }
                case LEFT -> {
                    setX(MathUtils.clamp(xOffset + (int)componentPos.x() + parentComponent.width() + distanceToParent, 0, Minecraft.getInstance().screen.width - width() - distanceToParent));
                    setY(MathUtils.clamp(yOffset + (int)componentPos.y() + parentComponent.height() / 2 - height() / 2, 0, Minecraft.getInstance().screen.height - height()));
                }
            }
            contentArea.clear();
            animator.start(3, null, null, () -> {
                enabled.set(true);
            });
            return false;
        });

        addEventListener(DLGuiStandardEvents.RenderPreEvent.class, (s, e) -> {
            if (e.layer() != RenderLayer.MAIN) {
                return false;
            }
            renderBasePre(e.graphics(), e.mouseX(), e.mouseY(), e.renderBounds());
            return false;
        }, 100);
        

        addEventListener(DLGuiStandardEvents.RenderPostEvent.class, (s, e) -> {
            if (e.layer() != RenderLayer.MAIN) {
                return false;
            }
            renderBasePost(e.graphics(), e.mouseX(), e.mouseY(), e.renderBounds());
            return false;
        }, -100);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() {
        isClosing = true;
        enabled.set(false);
        animator.start(3, null, null, () -> {
            closeImmediately();
        });
    }

    public void closeImmediately() {
        isClosing = true;
        onClose();
        if (isClosed) {
            return;
        }
        isClosed = true;
        getWindowManager().closeModal(this.getAssignedModal().get());
    }

    protected void onOpen() {}
    protected void onClose() {}

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        
        renderFlyout(graphics, mouseX, mouseY, contentArea.get());
        renderFlyoutContent(graphics, mouseX, mouseY, contentArea.get());
    }

    public void renderBasePre(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        graphics.poseStack().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (animator.isRunning()) {
            if (isClosing) {
                switch (pointer) {
                    case UP -> graphics.poseStack().translate(0, -animator.getCurrentTicksSmooth() * 2, 0);
                    case DOWN -> graphics.poseStack().translate(0, animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0);
                    case LEFT -> graphics.poseStack().translate(animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0, 0);
                    case RIGHT -> graphics.poseStack().translate(animator.getCurrentTicksSmooth() * 2, 0, 0);
                }
                GuiUtils.setTint(DLColor.of(1f - animator.getPercentage(), 1, 1, 1));
            } else {
                switch (pointer) {
                    case UP -> graphics.poseStack().translate(0, -animator.getTotalTicks() * 2 + animator.getCurrentTicksSmooth() * 2, 0);
                    case DOWN -> graphics.poseStack().translate(0, animator.getCurrentTicksSmooth() * 2, 0);
                    case LEFT -> graphics.poseStack().translate(-animator.getCurrentTicksSmooth() * 2, 0, 0);
                    case RIGHT -> graphics.poseStack().translate(animator.getTotalTicks() * 2 - animator.getCurrentTicksSmooth() * 2, 0, 0);
                }
                GuiUtils.setTint(DLColor.of(animator.getPercentage(), 1, 1, 1));
            }
        }
        graphics.poseStack().pushPose();
    }

    public void renderBasePost(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        graphics.poseStack().popPose();
        graphics.poseStack().popPose();
        GuiUtils.resetTint();        
        RenderSystem.disableBlend();
    }
    
    public void renderFlyoutContent(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) { }


    public void renderFlyout(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) {
        CreateDynamicWidgets.renderShadow(graphics, (int)contentArea.x(), (int)contentArea.y(), (int)contentArea.width(), (int)contentArea.height());
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, (int)contentArea.x(), (int)contentArea.y(), (int)contentArea.width(), (int)contentArea.height(), pointerShade);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        switch (pointer) {
            case UP:
                pointer.render(graphics, width() / 2 - FlyoutPointer.WIDTH / 2, 0, pointerShade);
                break;
            case DOWN:
                pointer.render(graphics, width() / 2 - FlyoutPointer.WIDTH / 2, height() - FlyoutPointer.HEIGHT, pointerShade);
                break;
            case LEFT:
                pointer.render(graphics, 0, height() / 2 - FlyoutPointer.HEIGHT / 2, pointerShade);
                break;
            case RIGHT:
                pointer.render(graphics, width() - FlyoutPointer.WIDTH, height() / 2 - FlyoutPointer.HEIGHT / 2, pointerShade);
                break;
            default:
                break;
        }
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

        public void render(DLGuiGraphics graphics, int x, int y, ColorShade shade) {
            int u = shade == ColorShade.LIGHT ? getU() : getU() + WIDTH;
            GuiUtils.drawTexture(CRNGui.GUI, graphics, x, y, WIDTH, HEIGHT, u, v);
        }
    }
}
