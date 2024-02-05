package de.mrjulsen.crn.client.gui.screen;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.widgets.SettingsOptionWidget;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.gui.WidgetsCollection;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private final int AREA_Y = 16;        
    private final int AREA_W = 220;
    private final int AREA_H = 194;
    private GuiAreaDefinition workingArea;

    private int guiLeft, guiTop;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private final GlobalSettingsScreen instance;

    // Controls
    private IconButton backButton;
    private final WidgetsCollection optionsCollection = new WidgetsCollection();

    // Tooltips
    private final Component optionAliasTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_alias.title");
    private final Component optionAliasDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_alias.description");
    private final Component optionBlacklistTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_blacklist.title");
    private final Component optionBlacklistDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.option_blacklist.description");    
    private final Component optionTrainGroupTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.train_group.title");
    private final Component optionTrainGroupDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.train_group.description");    
    private final Component optionTrainBlacklistTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.train_blacklist.title");
    private final Component optionTrainBlacklistDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".global_settings.train_blacklist.description");

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
        workingArea = new GuiAreaDefinition(guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);

        optionsCollection.components.clear();

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY, optionAliasTitle, optionAliasDescription, (btn) -> {
            minecraft.setScreen(new AliasSettingsScreen(level, instance));
        }));

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY + SettingsOptionWidget.HEIGHT + ENTRY_SPACING, optionBlacklistTitle, optionBlacklistDescription, (btn) -> {
            minecraft.setScreen(new StationBlacklistScreen(level, instance));
        }));

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY + (SettingsOptionWidget.HEIGHT + ENTRY_SPACING) * 2, optionTrainGroupTitle, optionTrainGroupDescription, (btn) -> {
            minecraft.setScreen(new TrainGroupScreen(level, instance));
        }));

        optionsCollection.components.add(new SettingsOptionWidget(this, guiLeft + 26, startY + (SettingsOptionWidget.HEIGHT + ENTRY_SPACING) * 3, optionTrainBlacklistTitle, optionTrainBlacklistDescription, (btn) -> {
            minecraft.setScreen(new TrainBlacklistScreen(level, instance));
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
        
		scroll.tickChaser();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        float scrollOffset = -scroll.getValue(pPartialTick);
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);
        
        optionsCollection.performForEachOfType(SettingsOptionWidget.class, x -> x.renderButton(pPoseStack, pMouseX, (int)(pMouseY - scrollOffset), pPartialTick, workingArea.isInBounds(pMouseX, pMouseY)));

        pPoseStack.popPose();
        ModGuiUtils.endStencil();        
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10, 0x77000000, 0x00000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H, 0x00000000, 0x77000000);
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());


        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        double maxHeight = getMaxScrollHeight();
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
        }
        
        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {
		float scrollOffset = -scroll.getValue(pPartialTicks);
        ModGuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), pPoseStack, pMouseX, pMouseY, 0, 0);
        optionsCollection.performForEachOfType(SettingsOptionWidget.class, x -> workingArea.isInBounds(pMouseX, pMouseY), x -> x.renderForeground(pPoseStack, pMouseX, pMouseY, -scrollOffset));
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        float scrollOffset = -scroll.getValue();
        
        optionsCollection.performForEachOfType(SettingsOptionWidget.class,
            x -> workingArea.isInBounds(pMouseX, pMouseY) && x.isMouseOver(pMouseX, (int)(pMouseY - scrollOffset)),
            x -> x.mouseClicked(pMouseX, (int)(pMouseY - scrollOffset), pButton)
        );
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private int getMaxScrollHeight() {
        return (SettingsOptionWidget.HEIGHT + ENTRY_SPACING) * 4 + ENTRIES_START_Y_OFFSET * 2;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {

		float chaseTarget = scroll.getChaseTarget();		
        float max = -AREA_H + getMaxScrollHeight();

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
    
}
