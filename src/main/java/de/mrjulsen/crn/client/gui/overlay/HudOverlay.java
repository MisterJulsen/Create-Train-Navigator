package de.mrjulsen.crn.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraftforge.client.gui.ForgeIngameGui;

public interface HudOverlay {
    int getId();
    void render(ForgeIngameGui gui, PoseStack poseStack, int width, int height, float partialTicks);
    void tick();
}
