package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.DynamicWidgets;
import de.mrjulsen.crn.client.gui.MutableGuiAreaDefinition;
import de.mrjulsen.crn.client.gui.DynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class SearchSettingsScreen extends CommonScreen {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final int ENTRIES_START_Y_OFFSET = 10;
    private static final int ENTRY_HEIGHT = 62;
    private static final int ENTRY_SPACING = 4;
    private static final int DISPLAY_WIDTH = 164;
    
    private static final int ARRAY_ENTRY_HEIGHT = 20; 
    
    private final int AREA_X = 16;
    private final int AREA_Y = 16;        
    private final int AREA_W = 220;
    private final int AREA_H = 194;
    private GuiAreaDefinition workingArea;


    private int guiLeft, guiTop;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
    private int maxY = 0;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private final TrainGroup[] trainGroups;
    private final Map<TrainGroup, MutableGuiAreaDefinition> areaByTrainGroup = new HashMap<>();
    private int transferTimeInputInitialY = 0;
    private int transferTimeLabelInitialY = 0;

    private boolean trainGroupsExpanded;

    // Controls
    private IconButton backButton;
    private IconButton defaultsButton;
    private ScrollInput transferTimeInput;
    private Component transferLabel;
    private MultiLineLabel transferOptionLabel;
    private MultiLineLabel trainGroupsOptionLabel;
    
    private MutableGuiAreaDefinition trainGroupResetButton;
    private MutableGuiAreaDefinition trainGroupExpandButton;

    // Tooltips
    private final Component transferTimeBoxText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.transfer_time");
    private final Component transferTimeBoxDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.transfer_time.description");
    private final Component trainGroupsText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups");
    private final Component trainGroupsDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups.description");
    private final Component trainGroupsOverviewAll = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups.overview.all");
    private final Component trainGroupsOverviewNone = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups.overview.none");
    private final Component tooltipTrainGroupsReset = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups.tooltip.reset");
    private final String trainGroupsOverviewKey = "gui." + ModMain.MOD_ID + ".search_settings.train_groups.overview";


    @SuppressWarnings("resource")
    public SearchSettingsScreen(Level level, Screen lastScreen) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.title"));
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
        this.trainGroups = GlobalSettingsManager.getInstance().getSettingsData().getTrainGroupsList().toArray(TrainGroup[]::new);
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;    
        
        workingArea = new GuiAreaDefinition(guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });

        defaultsButton = this.addRenderableWidget(new IconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_REFRESH) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                ModClientConfig.resetSearchSettings();
                clearWidgets();
                init();
            }
        });

        transferTimeLabelInitialY = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 44;
        transferTimeInputInitialY = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39;
        transferTimeInput = addRenderableWidget(new ScrollInput(guiLeft + AREA_X + 10 + 25, transferTimeInputInitialY, 60, 18)
            .withRange(0, ModClientConfig.MAX_TRANSFER_TIME + 1)
            .withStepFunction(x -> 500 * (x.shift ? 2 : 1))
            .titled(transferTimeBoxText.copy())
            .calling((i) -> {
                ModClientConfig.TRANSFER_TIME.set(i);
                ModClientConfig.TRANSFER_TIME.save();
                ModClientConfig.SPEC.afterReload();
                transferLabel = new TextComponent(TimeUtils.parseDurationShort(transferTimeInput.getState()));
            })
            .setState(ModClientConfig.TRANSFER_TIME.get()));
        transferTimeInput.onChanged();
        transferOptionLabel = MultiLineLabel.create(shadowlessFont, transferTimeBoxDescription, (int)((DISPLAY_WIDTH) / 0.75f));


        trainGroupExpandButton = new MutableGuiAreaDefinition(0, 0, 16, 16);
        trainGroupResetButton = new MutableGuiAreaDefinition(0, 0, 16, 16);
        areaByTrainGroup.clear();
        for (int i = 0; i < trainGroups.length; i++) {
            TrainGroup group = trainGroups[i];
            areaByTrainGroup.put(group, new MutableGuiAreaDefinition(2, 0, 200 - 4, ARRAY_ENTRY_HEIGHT));
        }

        trainGroupsOptionLabel = MultiLineLabel.create(shadowlessFont, trainGroupsDescription, (int)((DISPLAY_WIDTH - 32) / 0.75f));
    }

    @Override
    public void onClose() {
        ModClientConfig.SPEC.save();
        ModClientConfig.SPEC.afterReload();
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
		scroll.tickChaser();
    }

    private void renderDefaultOptionWidget(PoseStack pPoseStack, int x, int y, String text, MultiLineLabel label) {
        pPoseStack.pushPose();
        drawString(pPoseStack, shadowlessFont, text, x + 25, y + 6, 0xFFFFFF);
        pPoseStack.scale(0.75f, 0.75f, 0.75f);        
        label.renderLeftAligned(pPoseStack, (int)((x + 25) / 0.75f), (int)((y + 19) / 0.75f), 10, 0xDBDBDB);
        pPoseStack.popPose();
    }


    @Override
    public void renderBg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        GuiUtils.blit(GUI, pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        float scrollOffset = -scroll.getValue(pPartialTick);

        // SCROLLABLE AREA START
        pPoseStack.pushPose();
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        GuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        final int defaultWidth = 200;
        final int defaultDescriptionHeight = 36;
        final int defaultOptionHeight = 26;
        int wX = guiLeft + AREA_X + 10;
        int wY = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET;

        // transfer time
        DynamicWidgets.renderDuoShadeWidget(pPoseStack, wX, wY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT, defaultOptionHeight, ColorShade.DARK);
        DynamicWidgets.renderTextSlotOverlay(pPoseStack, wX + 25, wY + 39, 163, 18);
        DynamicWidgets.renderTextBox(pPoseStack, wX + 25, wY + 39, 66);
        renderDefaultOptionWidget(pPoseStack, wX, wY, transferTimeBoxText.getString(), transferOptionLabel);
        drawString(pPoseStack, font, transferLabel, guiLeft + AREA_X + 10 + 30, transferTimeLabelInitialY, 0xFFFFFF);
        wY += ENTRY_SPACING + defaultOptionHeight + defaultDescriptionHeight;
        
        // train groups
        int dY = wY;
        if (trainGroupsExpanded) {
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT);
            DynamicWidgets.renderWidgetTopBorder(pPoseStack, wX, dY, defaultWidth);
            dY += defaultDescriptionHeight;
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, 2, ColorShade.DARK);
            dY += 2;
            
            for (int i = 0; i < trainGroups.length; i++) {
                if (dY + (i * ARRAY_ENTRY_HEIGHT) > workingArea.getTop() + workingArea.getHeight() - scrollOffset || dY + (i * ARRAY_ENTRY_HEIGHT) < workingArea.getTop() - ARRAY_ENTRY_HEIGHT - scrollOffset) {
                    continue;
                }

                TrainGroup group = trainGroups[i];
                MutableGuiAreaDefinition area = areaByTrainGroup.get(group);
                area.setXOffset(wX);
                area.setYOffset(dY + (i * ARRAY_ENTRY_HEIGHT));

                DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY + (i * ARRAY_ENTRY_HEIGHT), defaultWidth, 20, ColorShade.DARK);
                DynamicWidgets.renderTextSlotOverlay(pPoseStack, wX + 25, dY + (i * ARRAY_ENTRY_HEIGHT) + 1, 163, ARRAY_ENTRY_HEIGHT - 2);
                drawString(pPoseStack, shadowlessFont, group.getGroupName(), wX + 30, dY + (i * ARRAY_ENTRY_HEIGHT) + 1 + 5, 0xFFFFFF);
                
                DynamicWidgets.renderTextSlotOverlay(pPoseStack, wX + 6, dY + (i * ARRAY_ENTRY_HEIGHT) + 1, 16, ARRAY_ENTRY_HEIGHT - 2);
                
                if (ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.get().stream().noneMatch(x -> x.equals(group.getGroupName()))) {
                    AllIcons.I_CONFIRM.render(pPoseStack, wX + 6, dY + (i * ARRAY_ENTRY_HEIGHT) + 2);
                }

                if (workingArea.isInBounds(pMouseX, pMouseY) && area.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                    fill(pPoseStack, area.getX(), area.getY(), area.getRight(), area.getBottom(), 0x1AFFFFFF);
                }
            }
            dY += trainGroups.length * ARRAY_ENTRY_HEIGHT;            
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, 2, ColorShade.DARK);
            dY += 2;
            DynamicWidgets.renderWidgetBottomBorder(pPoseStack, wX, dY, defaultWidth);
        } else {
            DynamicWidgets.renderDuoShadeWidget(pPoseStack, wX, wY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT, defaultOptionHeight, ColorShade.DARK);
            int amount = trainGroups.length - ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.get().size();
            String text = String.valueOf(amount);
            if (amount <= 0) {
                text = trainGroupsOverviewNone.getString();
            } else if (amount >= trainGroups.length) {
                text = trainGroupsOverviewAll.getString();
            }
            drawString(pPoseStack, font, Utils.translate(trainGroupsOverviewKey, text), wX + 25, wY + defaultDescriptionHeight + defaultOptionHeight / 2 - font.lineHeight / 2, amount <= 0 ? 0xFF8888 : 0xFFFF88);
            
        }

        renderDefaultOptionWidget(pPoseStack, wX, wY, trainGroupsText.getString(), trainGroupsOptionLabel);
        trainGroupExpandButton.setXOffset(wX + defaultWidth - 2 - 16);
        trainGroupExpandButton.setYOffset(wY + defaultDescriptionHeight / 2 - 7);
        trainGroupResetButton.setXOffset(wX + defaultWidth - 2 - 32);
        trainGroupResetButton.setYOffset(wY + defaultDescriptionHeight / 2 - 7);
        AllIcons.I_REFRESH.render(pPoseStack, trainGroupResetButton.getX(), trainGroupResetButton.getY()); // delete button
        GuiUtils.blit(GUI_WIDGETS, pPoseStack, trainGroupExpandButton.getX(), trainGroupExpandButton.getY(), trainGroupsExpanded ? 216 : 200, 0, 16, 16); // expand button 
        // Button highlight
        if (workingArea.isInBounds(pMouseX, pMouseY)) {
            if (trainGroupExpandButton.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                fill(pPoseStack, trainGroupExpandButton.getX(), trainGroupExpandButton.getY(), trainGroupExpandButton.getRight(), trainGroupExpandButton.getBottom(), 0x1AFFFFFF);
            } else if (trainGroupResetButton.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                fill(pPoseStack, trainGroupResetButton.getX(), trainGroupResetButton.getY(), trainGroupResetButton.getRight(), trainGroupResetButton.getBottom(), 0x1AFFFFFF);
            }
        }

        wY += ENTRY_SPACING + dY;

        pPoseStack.popPose();
        GuiUtils.endStencil();
        
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10,
        0x77000000, 0x00000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H,
        0x00000000, 0x77000000);

        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

        // widgets y offset
        transferTimeInput.y = (int)(transferTimeInputInitialY + scrollOffset);

        // set scrollbar values
        maxY = wY - AREA_H;
        pPoseStack.popPose();
        // SCROLLABLE AREA END
        
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        double maxHeight = getMaxScrollHeight();
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
        }
    }

    @Override
    public void renderFg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        int scrollOffset = (int)scroll.getValue(pPartialTick);
        GuiUtils.renderTooltipWithScrollOffset(this, backButton, List.of(Constants.TOOLTIP_GO_BACK), width, pPoseStack, pMouseX, pMouseY, 0, 0);
        GuiUtils.renderTooltipWithScrollOffset(this, defaultsButton, List.of(Constants.TOOLTIP_RESET_DEFAULTS), width, pPoseStack, pMouseX, pMouseY, 0, 0);
        if (workingArea.isInBounds(pMouseX, pMouseY)) {
            GuiUtils.renderTooltipWithScrollOffset(this, trainGroupResetButton, List.of(tooltipTrainGroupsReset), width, pPoseStack, pMouseX, pMouseY, 0, scrollOffset);
        }

        for (Widget widget : renderables) {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isHoveredOrFocused()
                && simiWidget.visible) {
                List<Component> tooltip = simiWidget.getToolTip();
                if (tooltip.isEmpty())
                    continue;
                int ttx = simiWidget.lockedTooltipX == -1 ? pMouseX : simiWidget.lockedTooltipX + simiWidget.x;
                int tty = simiWidget.lockedTooltipY == -1 ? pMouseY : simiWidget.lockedTooltipY + simiWidget.y;
                renderComponentTooltip(pPoseStack, tooltip, ttx, tty);
            }
        }
        super.renderFg(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        float scrollOffset = -scroll.getValue(0);
        if (workingArea.isInBounds(pMouseX, pMouseY)) {
            if (trainGroupResetButton.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.set(new ArrayList<>());
                ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.save();
                ModClientConfig.SPEC.afterReload();
                ModGuiUtils.playButtonSound();
                return true;
            } else if (trainGroupExpandButton.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                trainGroupsExpanded = !trainGroupsExpanded;
                ModGuiUtils.playButtonSound();
                return true;
            }

            if (trainGroupsExpanded) {
                Optional<Entry<TrainGroup, MutableGuiAreaDefinition>> area = areaByTrainGroup.entrySet().stream().filter(x -> x.getValue().isInBounds(pMouseX, pMouseY - scrollOffset)).findFirst();
                if (area.isPresent()) {
                    List<String> current = new ArrayList<>(ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.get());
                    if (current.contains(area.get().getKey().getGroupName())) {
                        current.removeIf(x -> x.equals(area.get().getKey().getGroupName()));
                    } else {
                        current.add(area.get().getKey().getGroupName());                    
                    }
                    ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.set(current);
                    ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.save();
                    ModClientConfig.SPEC.afterReload();
                    ModGuiUtils.playButtonSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private int getMaxScrollHeight() {
        return maxY;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        
		boolean b = super.mouseScrolled(pMouseX, pMouseY, pDelta) || transferTimeInput.isHoveredOrFocused();
        if (b) {
            return true;
        }

		float chaseTarget = scroll.getChaseTarget();		
        float max = -AREA_H + getMaxScrollHeight();

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else {
			scroll.chase(0, 0.7f, Chaser.EXP);
        }

        return true;

    }
}
