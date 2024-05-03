package de.mrjulsen.crn.client.gui.screen;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.SettingsOptionWidget;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class GlobalSettingsScreen extends DLScreen {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/settings.png");
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
    private final Component optionAliasTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.title");
    private final Component optionAliasDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_alias.description");
    private final Component optionBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.title");
    private final Component optionBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.option_blacklist.description");    
    private final Component optionTrainGroupTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_group.title");
    private final Component optionTrainGroupDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_group.description");    
    private final Component optionTrainBlacklistTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.title");
    private final Component optionTrainBlacklistDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.train_blacklist.description");

    @SuppressWarnings("resource")
    public GlobalSettingsScreen(Level level, Screen lastScreen) {
        super(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".global_settings.title"));
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

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });
        addTooltip(DLTooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));
    }
    
    private int getMaxScrollHeight() {
        return (SettingsOptionWidget.HEIGHT + ENTRY_SPACING) * 4 + ENTRIES_START_Y_OFFSET * 2;
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
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        final float partialTicks = Minecraft.getInstance().getFrameTime();
        float scrollOffset = -scroll.getValue(partialTicks);

        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);          
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 19, guiTop + 4, title, 0x4F4F4F, EAlignment.LEFT, false);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, TextUtils.text(timeString), 0x4F4F4F, EAlignment.LEFT, false);

        // Scrollbar
        double maxHeight = getMaxScrollHeight();
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            GuiUtils.fill(graphics, guiLeft + AREA_X + AREA_W - 3, startY, 3, scrollerHeight, 0x7FFFFFFF);
        }

        // CONTENT
        GuiUtils.enableScissor(graphics, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        graphics.poseStack().translate(0, scrollOffset, 0);      
        
        optionsCollection.performForEachOfType(SettingsOptionWidget.class, x -> x.renderMainLayer(graphics, pMouseX, (int)(pMouseY - scrollOffset), partialTicks/*, workingArea.isInBounds(pMouseX, pMouseY)*/));
    
        GuiUtils.disableScissor(graphics);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y, 200, AREA_W, 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, 200, AREA_W, 10, 0x00000000, 0x77000000);

        super.renderMainLayer(graphics, pMouseX, pMouseY, partialTicks);        
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTicks) {
		float scrollOffset = -scroll.getValue(pPartialTicks);
        optionsCollection.performForEachOfType(SettingsOptionWidget.class, x -> workingArea.isInBounds(pMouseX, pMouseY), x -> x.renderFrontLayer(graphics, pMouseX, pMouseY, -scrollOffset));
        super.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTicks);
    }

    @Override
    public void mouseSelectEvent(int mouseX, int mouseY) {
        
        super.mouseSelectEvent(mouseX, mouseY);
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
