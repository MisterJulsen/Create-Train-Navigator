package de.mrjulsen.crn.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import de.mrjulsen.crn.ModMain;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public class LoadingScreen extends Screen {

    int angle = 0;
    
    public LoadingScreen() {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".loading.title"));
    }

    @Override
    public void tick() {
        angle += 6;
        if (angle > 360) {
            angle = 0;
        }
        super.tick();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
        renderBackground(pPoseStack);
        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5; 
        
        drawCenteredString(pPoseStack, font, title, width / 2, height / 2, 0xFFFFFF);
        AllIcons.I_MTD_SCAN.render(pPoseStack, (int)(width / 2 + offsetX), (int)(height / 2 - 50 + offsetY));
    }    
}
