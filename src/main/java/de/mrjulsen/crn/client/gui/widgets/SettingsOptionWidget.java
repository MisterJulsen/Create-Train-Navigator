package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class SettingsOptionWidget extends Button implements IForegroundRendering {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;

    private static final int DISPLAY_WIDTH = 190;
    
    private final Screen parent;
    private final Font shadowlessFont;

    // Controls
    private final MultiLineLabel messageLabel;
    
    private final TranslatableComponent tooltipOptionText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option.tooltip");
    

    public SettingsOptionWidget(Screen parent, int pX, int pY, Component title, Component description, OnPress pOnPress) {
        super(pX, pY, 200, 48, title, pOnPress);
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.parent = parent;
        this.messageLabel = MultiLineLabel.create(shadowlessFont, description, (int)((DISPLAY_WIDTH) / 0.75f));
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        
        float l = isMouseOver(pMouseX, pMouseY) ? 0.2f : 0;
        RenderSystem.setShaderColor(1 + l, 1 + l, 1 + l, 1);
        RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 0, WIDTH, HEIGHT);


        drawString(pPoseStack, shadowlessFont, getMessage(), x + 6, y + 5, 0xFFFFFF);
        pPoseStack.scale(0.75f, 0.75f, 0.75f);        
        this.messageLabel.renderLeftAligned(pPoseStack, (int)((x + 6) / 0.75f), (int)((y + 20) / 0.75f), 10, 0xDBDBDB);
        float s = 1 / 0.75f;
        pPoseStack.scale(s, s, s);
    }

    @Override
    public void renderForeground(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {
        GuiUtils.renderTooltip(parent, this, List.of(tooltipOptionText.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, (int)pPartialTicks);
    }
    
}
