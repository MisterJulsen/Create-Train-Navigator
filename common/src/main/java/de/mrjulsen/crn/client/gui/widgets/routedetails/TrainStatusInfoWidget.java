package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Widget showing one reason a train is delayed or out of service, named and nothing more. What is
 * specific to the occurrence - the train in the way, the blocked platform - is deliberately left out
 * and stays available on the {@link DelayInstance} for a view that wants to spell it out.
 */
public class TrainStatusInfoWidget extends DLGuiComponent {

    private static final int MIN_HEIGHT = 9;
    private static final float SCALE = 0.75f;

    private final Component text;
    private final DLColor color;
    private boolean loopFix = false;

    public TrainStatusInfoWidget(int x, int y, int w, DelayInstance delay) {
        super(x, y, w, 1);
        this.text = TextUtils.translate(delay.translationKey());
        this.color = delay.severity().color();

        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            if (loopFix) return false;
            loopFix = true;
            update();
            loopFix = false;
            return false;
        });

        update();
    }

    private void update() {
        this.setHeight(Math.max(MIN_HEIGHT, (int)(ClientWrapper.getTextBlockHeight(Minecraft.getInstance().font, text, textWidth()) * SCALE) + 2));
    }

    private int textWidth() {
        return (int)((width() - ModGuiIcons.ICON_SIZE) / SCALE);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        graphics.poseStack().pushPose();
        GuiUtils.setTint(color);
        ModGuiIcons.IMPORTANT.render(graphics, -4, -3);
        graphics.poseStack().scale(SCALE, SCALE, 1);
        ClientWrapper.renderMultilineLabelSafe(graphics, (int)(10 / SCALE), (int)(2 / SCALE), graphics.defaultFont(), text, textWidth(), color);
        graphics.poseStack().popPose();
        GuiUtils.resetTint();
    }
}
