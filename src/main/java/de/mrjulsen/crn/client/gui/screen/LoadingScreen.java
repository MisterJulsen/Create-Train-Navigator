package de.mrjulsen.crn.client.gui.screen;

import com.simibubi.create.foundation.gui.AllIcons;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.gui.GuiGraphics;

public class LoadingScreen extends CommonScreen {

    int angle = 0;
    
    public LoadingScreen() {
        super(Utils.translate("gui." + ModMain.MOD_ID + ".loading.title"));
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
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        
        renderBackground(graphics);
        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5; 
        
        graphics.drawCenteredString(font, title, width / 2, height / 2, 0xFFFFFF);
        AllIcons.I_MTD_SCAN.render(graphics, (int)(width / 2 + offsetX), (int)(height / 2 - 50 + offsetY));
    }    
}
