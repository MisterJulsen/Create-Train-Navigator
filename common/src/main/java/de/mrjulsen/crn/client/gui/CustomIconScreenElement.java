package de.mrjulsen.crn.client.gui;

import com.simibubi.create.foundation.gui.element.ScreenElement;

import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CustomIconScreenElement implements ScreenElement {

    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int uW;
    private final int vH;
    private final int texW;
    private final int texH;

    public CustomIconScreenElement(int u, int v, int uW, int vH, int texW, int texH, ResourceLocation texture) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.uW = uW;
        this.vH = vH;
        this.texW = texW;
        this.texH = texH;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y) {
        GuiUtils.drawTexture(texture, new Graphics(guiGraphics, guiGraphics.pose()), x, y, u, v, uW, vH, texW, texH);
    }
    
}
