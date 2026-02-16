package de.mrjulsen.crn.client.gui.widgets.create;

import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.createmod.catnip.gui.element.ScreenElement;

public class CreateButton extends DLButton {

    private static final DLTexture TEXTURE = new DLTexture(DLUtils.resourceLocation("create", "textures/gui/widgets.png"), 256, 256);
    public static final int WIDTH = 18;
    public static final int HEIGHT = 18;
    public ScreenElement icon;

    public CreateButton(int x, int y, ScreenElement icon) {
        super(x, y, WIDTH, HEIGHT);
        this.icon = icon;
        this.cursor.set(CursorType.HAND);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (!enabled.get()) {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, WIDTH, HEIGHT, WIDTH * 2, 0);
        } else if (isSelected()) {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, WIDTH, HEIGHT, WIDTH, 0);
        } else {
            GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, WIDTH, HEIGHT, 0, 0);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        icon.render(graphics.graphics(), 1, 1);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }
    
}
