package de.mrjulsen.crn.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

public interface IForegroundRendering {
    void renderForeground(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks);
}
