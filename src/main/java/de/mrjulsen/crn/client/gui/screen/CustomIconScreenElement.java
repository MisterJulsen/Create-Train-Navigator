package de.mrjulsen.crn.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.element.ScreenElement;

import net.minecraft.client.gui.GuiComponent;
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
    public void render(PoseStack ms, int x, int y) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(ms, x, y, u, v, uW, vH, texW, texH);
    }
    
}
