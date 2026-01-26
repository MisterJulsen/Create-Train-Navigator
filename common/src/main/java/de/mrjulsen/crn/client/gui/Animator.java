package de.mrjulsen.crn.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public class Animator extends DLGuiComponent {

    private int maxTicks;
    private int currentTicks;
    private float currentTicksSmooth;
    private boolean running;

    private IAnimatorRenderCallback onAnimateRender;
    private IAnimatorTickCallback onAnimateTick;
    private Runnable onCompleted;

    public Animator() {
        super(0, 0, 1, 1);
    }

    public boolean isRunning() {
        return running;
    }

    public int getTotalTicks() {
        return maxTicks;
    }

    public int getCurrentTicks() {
        return currentTicks;
    }

    public float getCurrentTicksSmooth() {
        return currentTicksSmooth;
    }

    public float getPercentage() {
        return 1F / (float)maxTicks * currentTicksSmooth;
    }

    @Override
    public Rectangle getRenderBounds() {
        return Rectangle.INFINITE;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (running) {
            float frameTime = Minecraft.getInstance().getDeltaFrameTime();
            currentTicksSmooth += frameTime;
            DLUtils.doIfNotNull(onAnimateRender, x -> x.execute(graphics.poseStack(), getCurrentTicks(), getTotalTicks(), getPercentage()));
            if (currentTicks >= maxTicks) {
                stop();
                DLUtils.doIfNotNull(onCompleted, x -> x.run());
            }
        }
    }

    @Override
    public void tick() {
        if (running) {
            DLUtils.doIfNotNull(onAnimateTick, x -> x.execute(getCurrentTicks(), getTotalTicks(), getPercentage()));
            currentTicks++;
            currentTicksSmooth = currentTicks;
            if (currentTicks >= maxTicks) {
                currentTicksSmooth = maxTicks;
                stop();
                DLUtils.doIfNotNull(onCompleted, x -> x.run());
            }
        }
    }

    public void start(int ticks, IAnimatorRenderCallback renderCallback, IAnimatorTickCallback tickCallback, Runnable onCompleted) {
        this.currentTicks = 0;
        this.currentTicksSmooth = 0;
        this.maxTicks = ticks;
        this.onAnimateRender = renderCallback;
        this.onAnimateTick = tickCallback;
        this.onCompleted = onCompleted;
        this.running = true;
    }

    public void stop() {
        this.running = false;
        this.currentTicks = 1;
        this.currentTicksSmooth = 1;
        this.maxTicks = 1;
    }
    
    @FunctionalInterface
    public static interface IAnimatorRenderCallback {
        void execute(PoseStack poseStack, int currentTicks, int totalTicks, double percentage);
    }
    
    @FunctionalInterface
    public static interface IAnimatorTickCallback {
        void execute(int currentTicks, int totalTicks, double percentage);
    }
}