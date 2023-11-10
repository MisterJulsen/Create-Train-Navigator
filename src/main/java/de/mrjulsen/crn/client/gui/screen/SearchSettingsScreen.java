package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.EFilterCriteria;
import de.mrjulsen.crn.data.EResultCount;
import de.mrjulsen.crn.util.GuiUtils;
import de.mrjulsen.crn.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
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
    
    private final int AREA_X = 16;
    private final int AREA_Y = 16;        
    //private final int AREA_W = 220;
    //private final int AREA_H = 195;


    private int guiLeft, guiTop;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;

    // Controls
    private IconButton backButton;
    private IconButton defaultsButton;
    private ScrollInput criteriaInput;
    private ScrollInput resultCountInput;
    private ScrollInput countScrollInput;
    private Label filterLabel;
    private Label resultCountLabel;
    private Label countLabel;
    private MultiLineLabel filterOptionLabel;
    private MultiLineLabel resultCountOptionLabel;
    private MultiLineLabel nextTrainOptionLabel;

    // Tooltips
    private final Component filterSelectionBoxText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.filter_selection");
    private final Component filterSelectionBoxDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.filter_selection.description");
    private final Component resultCountText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.result_count");
    private final Component resultCountDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.result_count.description");    
    private final Component nextTrainText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.next_train");
    private final Component nextTrainDescription = new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.next_train.description");


    @SuppressWarnings("resource")
    public SearchSettingsScreen(Level level, Screen lastScreen) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".search_settings.title"));
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
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
                //Settings.getInstance().setDefaults();
                ModClientConfig.reset();
                clearWidgets();
                init();
            }
        });

        filterLabel = addRenderableWidget(new Label(guiLeft + AREA_X + 10 + 30, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 44, Components.immutableEmpty()).withShadow());
        criteriaInput = addRenderableWidget(new SelectionScrollInput(guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (0 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, DISPLAY_WIDTH, 18)
            .forOptions(Arrays.stream(EFilterCriteria.values()).map(x -> new TranslatableComponent(x.getTranslationKey())).toList())
            .writingTo(filterLabel)
            .titled(filterSelectionBoxText.copy())
            .calling((i) -> {
                ModClientConfig.FILTER_CRITERIA.set(EFilterCriteria.getCriteriaById(i));
                ModClientConfig.FILTER_CRITERIA.save();
                ModClientConfig.SPEC.afterReload();
            })
            .setState(ModClientConfig.FILTER_CRITERIA.get().getId()));
        criteriaInput.onChanged();        
        filterOptionLabel = MultiLineLabel.create(shadowlessFont, filterSelectionBoxDescription, (int)((DISPLAY_WIDTH) / 0.75f));


        countLabel = addRenderableWidget(new Label(guiLeft + AREA_X + 10 + 30 + 104, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (1 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 44, Components.immutableEmpty()).withShadow());
        countScrollInput = addRenderableWidget(new ScrollInput(guiLeft + AREA_X + 10 + 25 + 104, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (1 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 30, 18)
            .withRange(1, 1000)
            .withShiftStep(10)
            .writingTo(countLabel)
            .titled(Constants.TEXT_COUNT.copy())
            .calling((i) -> {
                ModClientConfig.RESULT_AMOUNT.set(i);
                ModClientConfig.RESULT_AMOUNT.save();
                ModClientConfig.SPEC.afterReload();
            })
            .setState(ModClientConfig.RESULT_AMOUNT.get()));
        countScrollInput.onChanged();


        resultCountLabel = addRenderableWidget(new Label(guiLeft + AREA_X + 10 + 30, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (1 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 44, Components.immutableEmpty()).withShadow());
        resultCountInput = addRenderableWidget(new SelectionScrollInput(guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (1 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 100, 18)
            .forOptions(Arrays.stream(EResultCount.values()).map(x -> new TranslatableComponent(x.getTranslationKey())).toList())
            .writingTo(resultCountLabel)
            .titled(resultCountText.copy())
            .calling((i) -> {
                EResultCount count = EResultCount.getCriteriaById(i);
                countScrollInput.visible = count == EResultCount.FIXED_AMOUNT;
                countLabel.visible = count == EResultCount.FIXED_AMOUNT;
                ModClientConfig.RESULT_RANGE.set(count);
                ModClientConfig.RESULT_RANGE.save();
                ModClientConfig.SPEC.afterReload();
            })
            .setState(ModClientConfig.RESULT_RANGE.get().getId()));
        resultCountInput.onChanged();
        resultCountOptionLabel = MultiLineLabel.create(shadowlessFont, resultCountDescription, (int)((DISPLAY_WIDTH) / 0.75f));

        nextTrainOptionLabel = MultiLineLabel.create(shadowlessFont, nextTrainDescription, (int)((DISPLAY_WIDTH) / 0.75f));
        Checkbox box = new Checkbox(guiLeft + AREA_X + 10 + 5, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (2 * (ENTRY_HEIGHT + ENTRY_SPACING)) + 5, 20, 20, new TextComponent(""), ModClientConfig.TAKE_NEXT_DEPARTING_TRAIN.get()) {
            public void onPress() {
                super.onPress();
                ModClientConfig.TAKE_NEXT_DEPARTING_TRAIN.set(this.selected());
                ModClientConfig.TAKE_NEXT_DEPARTING_TRAIN.save();
                ModClientConfig.SPEC.afterReload();
            };
        };
        addRenderableWidget(box);
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

    private void renderOptionField(PoseStack pPoseStack, int x, int y, String text, MultiLineLabel label) {
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 138, 200, ENTRY_HEIGHT);
        pPoseStack.pushPose();
        drawString(pPoseStack, shadowlessFont, text, x + 25, y + 6, 0xFFFFFF);
        pPoseStack.scale(0.75f, 0.75f, 0.75f);        
        label.renderLeftAligned(pPoseStack, (int)((x + 25) / 0.75f), (int)((y + 19) / 0.75f), 10, 0xDBDBDB);
        float s = 1 / 0.75f;
        pPoseStack.scale(s, s, s);
        pPoseStack.popPose();
    }

    private void renderEntryField(PoseStack pPoseStack, int x, int y, int w) {
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 92, w / 2, 18);      
        blit(pPoseStack, x + w / 2, y, 139 - w / 2, 92, w / 2, 18);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        int index = 0;
        // sorting
        renderOptionField(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)), filterSelectionBoxText.getString(), filterOptionLabel);
        renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, DISPLAY_WIDTH);
        
        // count
        index++;
        renderOptionField(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)), resultCountText.getString(), resultCountOptionLabel);
        renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 100);
        if (ModClientConfig.RESULT_RANGE.get() == EResultCount.FIXED_AMOUNT) {
            renderEntryField(pPoseStack, guiLeft + AREA_X + 10 + 25 + 104, guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING)) + 39, 30);
        }

        // next train
        index++;
        int x = guiLeft + AREA_X + 10;
        int y = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET + (index * (ENTRY_HEIGHT + ENTRY_SPACING));

        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 138, 200, 36);
        blit(pPoseStack, x, y + 36, 0, 46, 200, 2);
        pPoseStack.pushPose();
        drawString(pPoseStack, shadowlessFont, nextTrainText, x + 30, y + 6, 0xFFFFFF);
        pPoseStack.scale(0.75f, 0.75f, 0.75f);        
        nextTrainOptionLabel.renderLeftAligned(pPoseStack, (int)((x + 30) / 0.75f), (int)((y + 19) / 0.75f), 10, 0xDBDBDB);
        float s = 1 / 0.75f;
        pPoseStack.scale(s, s, s);
        pPoseStack.popPose();
        
        
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = Utils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY));
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {		
        GuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        GuiUtils.renderTooltip(this, defaultsButton, List.of(Constants.TOOLTIP_RESET_DEFAULTS.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);

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
}
