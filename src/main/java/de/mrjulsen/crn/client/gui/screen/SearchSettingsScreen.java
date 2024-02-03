package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.DynamicWidgets;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.MutableGuiAreaDefinition;
import de.mrjulsen.crn.client.gui.DynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.util.Utils;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class SearchSettingsScreen extends Screen implements IForegroundRendering {

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
    //private final int AREA_W = 220;
    //private final int AREA_H = 195;


    private int guiLeft, guiTop;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private final TrainGroup[] trainGroups;
    private final Map<TrainGroup, MutableGuiAreaDefinition> areaByTrainGroup = new HashMap<>();

    private boolean trainGroupsExpanded;

    // Controls
    private IconButton backButton;
    private IconButton defaultsButton;
    private ScrollInput transferTimeInput;
    private Label transferLabel;
    private MultiLineLabel transferOptionLabel;
    private MultiLineLabel trainGroupsOptionLabel;
    
    private GuiAreaDefinition trainGroupResetButton;
    private GuiAreaDefinition trainGroupExpandButton;

    // Tooltips
    private final Component transferTimeBoxText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.transfer_time");
    private final Component transferTimeBoxDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.transfer_time.description");
    private final Component trainGroupsText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups");
    private final Component trainGroupsDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.train_groups.description");


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
                ModClientConfig.reset();
                clearWidgets();
                init();
            }
        });

        transferLabel = addRenderableWidget(new Label(guiLeft + AREA_X + 10 + 30, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 44, Components.immutableEmpty()).withShadow());
        transferTimeInput = addRenderableWidget(new ScrollInput(guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 60, 18)
            .withRange(0, ModClientConfig.MAX_TRANSFER_TIME + 1)
            .withStepFunction(x -> 500 * (x.shift ? 2 : 1))
            .writingTo(transferLabel)
            .titled(transferTimeBoxText.copy())
            .format(x -> new TextComponent(Utils.parseDurationShort(x)))
            .calling((i) -> {
                ModClientConfig.TRANSFER_TIME.set(i);
                ModClientConfig.TRANSFER_TIME.save();
                ModClientConfig.SPEC.afterReload();
            })
            .setState(ModClientConfig.TRANSFER_TIME.get()));
        transferTimeInput.onChanged();
        transferOptionLabel = MultiLineLabel.create(shadowlessFont, transferTimeBoxDescription, (int)((DISPLAY_WIDTH) / 0.75f));


        /**
         * INSERT HERE
         * Gui for train groups filter
         */
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
    }

    private void renderDefaultOptionWidget(PoseStack pPoseStack, int x, int y, String text, MultiLineLabel label) {
        pPoseStack.pushPose();
        drawString(pPoseStack, shadowlessFont, text, x + 25, y + 6, 0xFFFFFF);
        pPoseStack.scale(0.75f, 0.75f, 0.75f);        
        label.renderLeftAligned(pPoseStack, (int)((x + 25) / 0.75f), (int)((y + 19) / 0.75f), 10, 0xDBDBDB);
        pPoseStack.popPose();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        final int defaultWidth = 200;
        final int defaultDescriptionHeight = 36;
        final int defaultOptionHeight = 26;
        int wX = guiLeft + AREA_X + 10;
        int wY = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET;

        // transfer
        DynamicWidgets.renderDuoShadeWidget(pPoseStack, wX, wY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT, defaultOptionHeight, ColorShade.DARK);
        DynamicWidgets.renderTextSlotOverlay(pPoseStack, wX + 25, wY + 39, 163, 18);
        DynamicWidgets.renderTextBox(pPoseStack, wX + 25, wY + 39, 66);
        renderDefaultOptionWidget(pPoseStack, wX, wY, transferTimeBoxText.getString(), transferOptionLabel);
        wY += ENTRY_SPACING + defaultOptionHeight + defaultDescriptionHeight;
        //renderOptionField(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)), transferTimeBoxText.getString(), transferOptionLabel);
        //renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 66);
        
        // train groups       
        if (trainGroupsExpanded) {
            int dY = wY;
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT);
            DynamicWidgets.renderWidgetTopBorder(pPoseStack, wX, dY, defaultWidth);
            dY += defaultDescriptionHeight;
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, 2, ColorShade.DARK);
            dY += 2;
            
            for (int i = 0; i < trainGroups.length; i++) {
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

                if (area.isInBounds(pMouseX, pMouseY)) {
                    fill(pPoseStack, area.getX(), area.getY(), area.getRight(), area.getBottom(), 0x1AFFFFFF);
                }
            }
            dY += trainGroups.length * ARRAY_ENTRY_HEIGHT;            
            DynamicWidgets.renderWidgetInner(pPoseStack, wX, dY, defaultWidth, 2, ColorShade.DARK);
            dY += 2;
            DynamicWidgets.renderWidgetBottomBorder(pPoseStack, wX, dY, defaultWidth);
        } else {
            DynamicWidgets.renderDuoShadeWidget(pPoseStack, wX, wY, defaultWidth, defaultDescriptionHeight, ColorShade.LIGHT, defaultOptionHeight, ColorShade.DARK);
        }
        renderDefaultOptionWidget(pPoseStack, wX, wY, trainGroupsText.getString(), trainGroupsOptionLabel);
        trainGroupExpandButton = new GuiAreaDefinition(wX + defaultWidth - 2 - 16, wY + defaultDescriptionHeight / 2 - 7, 16, 16);
        trainGroupResetButton = new GuiAreaDefinition(wX + defaultWidth - 2 - 32, wY + defaultDescriptionHeight / 2 - 7, 16, 16);
        AllIcons.I_REFRESH.render(pPoseStack, trainGroupResetButton.getX(), trainGroupResetButton.getY()); // delete button
        GuiUtils.blit(GUI_WIDGETS, pPoseStack, trainGroupExpandButton.getX(), trainGroupExpandButton.getY(), trainGroupsExpanded ? 216 : 200, 0, 16, 16); // expand button 
        // Button highlight
        if (trainGroupExpandButton.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, trainGroupExpandButton.getX(), trainGroupExpandButton.getY(), trainGroupExpandButton.getRight(), trainGroupExpandButton.getBottom(), 0x1AFFFFFF);
        } else if (trainGroupResetButton.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, trainGroupResetButton.getX(), trainGroupResetButton.getY(), trainGroupResetButton.getRight(), trainGroupResetButton.getBottom(), 0x1AFFFFFF);
        }
        //DynamicWidgets.renderTextSlotOverlay(pPoseStack, guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 163, 18);
        /*
        renderOptionField(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)), trainGroupsText.getString(), trainGroupsOptionLabel);
        renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 100);
        if (ModClientConfig.RESULT_RANGE.get() == EResultCount.FIXED_AMOUNT) {
            renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25 + 104, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 30);
        }
        */
        
        // other
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = Utils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY));
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {		
        de.mrjulsen.crn.util.GuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        de.mrjulsen.crn.util.GuiUtils.renderTooltip(this, defaultsButton, List.of(Constants.TOOLTIP_RESET_DEFAULTS.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);

        for (Widget widget : renderables) {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isHoveredOrFocused()
                && simiWidget.visible) {
                List<Component> tooltip = simiWidget.getToolTip();
                if (tooltip.isEmpty())
                    continue;
                int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.x;
                int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.y;
                renderComponentTooltip(matrixStack, tooltip, ttx, tty);
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (trainGroupResetButton.isInBounds(pMouseX, pMouseY)) {
            ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.set(new ArrayList<>());
            ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.save();
            ModClientConfig.SPEC.afterReload();
            return true;
        } else if (trainGroupExpandButton.isInBounds(pMouseX, pMouseY)) {
            trainGroupsExpanded = !trainGroupsExpanded;
            return true;
        }

        if (trainGroupsExpanded) {
            Optional<Entry<TrainGroup, MutableGuiAreaDefinition>> area = areaByTrainGroup.entrySet().stream().filter(x -> x.getValue().isInBounds(pMouseX, pMouseY)).findFirst();
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
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}
