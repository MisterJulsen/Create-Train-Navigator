package de.mrjulsen.crn.client.gui.screen;

import com.simibubi.create.foundation.gui.AllIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class LoadingScreen extends DLScreen {

    int angle = 0;
    
    public LoadingScreen() {
        super(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".loading.title"));
    }

    @Override
    protected void init() {
        super.init();
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
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

        renderScreenBackground(graphics);

        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5; 
        
        GuiUtils.drawString(graphics, font, width / 2, height / 2, title, 0xFFFFFF, EAlignment.CENTER, true);
        AllIcons.I_MTD_SCAN.render(graphics.graphics(), (int)(width / 2 + offsetX), (int)(height / 2 - 50 + offsetY));
    }
}
