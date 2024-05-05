package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SettingsOptionWidget extends DLButton {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;

    private static final int DISPLAY_WIDTH = 190;
    
    private final Screen parent;
    private final Font shadowlessFont;

    // Controls
    private final MultiLineLabel messageLabel;    
    private final MutableComponent tooltipOptionText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option.tooltip");
    

    public SettingsOptionWidget(Screen parent, int pX, int pY, Component title, Component description, Consumer<SettingsOptionWidget> onClick) {
        super(pX, pY, 200, 48, title, onClick);
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.parent = parent;
        this.messageLabel = MultiLineLabel.create(shadowlessFont, description, (int)((DISPLAY_WIDTH) / 0.75f));
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {        
        float l = isMouseOver(pMouseX, pMouseY) && isHovered ? 0.2f : 0;
        GuiUtils.setTint(1 + l, 1 + l, 1 + l, 1);
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, getX(), getY(), 0, 0, WIDTH, HEIGHT);

        GuiUtils.drawString(graphics, shadowlessFont, getX() + 6, getY() + 5, getMessage(), 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().scale(0.75f, 0.75f, 0.75f);        
        this.messageLabel.renderLeftAligned(graphics.graphics(), (int)((getX() + 6) / 0.75f), (int)((getY() + 20) / 0.75f), 10, 0xDBDBDB);
        float s = 1 / 0.75f;
        graphics.poseStack().scale(s, s, s);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTicks) {
        GuiUtils.renderTooltipWithOffset(parent, this, List.of(tooltipOptionText), parent.width, graphics, pMouseX, pMouseY, 0, (int)pPartialTicks);
    }
    
}
