package de.mrjulsen.crn.client.gui.overlay;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SlidingTextComponent extends DLGuiComponent {

    public final float SCALE = 1f / 0.75F;    
    private int textWidth = 0;

    public final Property<Component> text = new Property<Component>(TextUtils.empty())
        .withAfterPropertyChangedCallback((o, n) -> {
            textWidth = (int)(Minecraft.getInstance().font.width(n) * SCALE);
        });

    public SlidingTextComponent(int x, int y, int w) {
        super(x, y, w, 21);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        boolean needsScrolling = textWidth > width() - 10;
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(needsScrolling ? width() - (float)(System.currentTimeMillis() % ((textWidth + width()) * 20)) / 20f : width() / 2, height() / 2 - graphics.defaultFont().lineHeight / 2, 0);
        graphics.poseStack().scale(SCALE, SCALE, SCALE);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, text.get(), DLColor.fromInt(0xFFFF9900), needsScrolling ? ETextAlignment.LEFT : ETextAlignment.CENTER, false);        
        graphics.poseStack().popPose();
    }
    
}
