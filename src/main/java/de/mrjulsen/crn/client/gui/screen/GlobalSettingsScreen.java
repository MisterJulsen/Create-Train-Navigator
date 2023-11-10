package de.mrjulsen.crn.client.gui.screen;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.widgets.SettingsOptionWidget;
import de.mrjulsen.crn.util.GuiUtils;
import de.mrjulsen.crn.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class GlobalSettingsScreen extends Screen implements IForegroundRendering {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 10;
    private final int ENTRY_SPACING = 4;
    
    private final int AREA_X = 16;
    //private final int AREA_Y = 15;        
    //private final int AREA_W = 220;
    //private final int AREA_H = 195;

    private int guiLeft, guiTop;    

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private final GlobalSettingsScreen instance;

    // Controls
    private IconButton backButton;
    private final ControlCollection optionsCollection = new ControlCollection();

    // Tooltips
    private final Component optionAliasTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_alias.title");
    private final Component optionAliasDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_alias.description");    
    private final Component optionBlacklistTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_blacklist.title");
    private final Component optionBlacklistDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_blacklist.description");

    @SuppressWarnings("resource")
    public GlobalSettingsScreen(Level level, Screen lastScreen) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.title"));
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
        this.instance = this;
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;
        int startY = guiTop + AREA_X + ENTRIES_START_Y_OFFSET;

        optionsCollection.components.clear();

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY, optionAliasTitle, optionAliasDescription, (btn) -> {
            minecraft.setScreen(new AliasSettingsScreen(level, instance));
        }));

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY + SettingsOptionWidget.HEIGHT + ENTRY_SPACING, optionBlacklistTitle, optionBlacklistDescription, (btn) -> {
            minecraft.setScreen(new StationBlacklistScreen(level, instance));
        }));

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        optionsCollection.performForEach(x -> x.render(pPoseStack, pMouseX, pMouseY, pPartialTick));

        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = Utils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY));
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        
        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {
        GuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, 0);
        optionsCollection.performForEachOfType(IForegroundRendering.class, x -> x.renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTicks));
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        optionsCollection.performForEach(x -> x.isMouseOver(pMouseX, pMouseY), x -> x.mouseClicked(pMouseX, pMouseY, pButton));
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
    
}
