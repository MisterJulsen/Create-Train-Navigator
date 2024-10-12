package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateLabel;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TrainSectionSettingsScreen extends DLScreen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/section_settings.png");
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 143;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
	private static final ItemStack DISPLAY_ITEM = new ItemStack(AllItems.SCHEDULE.get());

    private final CompoundTag nbt;
    private final Screen lastScreen;

    // Settings
    private boolean includePreviousStation = false;
    private boolean usable = true;
    private String trainGroupId;
    private String trainLineId;

    private Map<String, TrainGroup> groupsById;
    private Map<String, TrainLine> linesById;

    // GUI
    private int guiLeft;
    private int guiTop;

    private ScrollInput infoTypeInput;
    private Label infoTypeLabel;
    private ScrollInput displayTypeInput;
    private Label displayTypeLabel;
    private DLCreateIconButton backButton;
    private DLCreateIconButton globalSettingsButton;

    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipTrainGroup = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_groups");
    private final MutableComponent tooltipTrainLine = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_lines");
    private final MutableComponent textIncludePreviousStation = TextUtils.translate("gui.createrailwaysnavigator.section_settings.include_previous_station");
    private final MutableComponent textUsable = TextUtils.translate("gui.createrailwaysnavigator.section_settings.usable");
    private final MutableComponent textNone = TextUtils.translate("gui.createrailwaysnavigator.section_settings.none");

    public TrainSectionSettingsScreen(Screen lastScreen, CompoundTag nbt) {
        super(TextUtils.translate("gui.createrailwaysnavigator.section_settings.title"));
        this.lastScreen = lastScreen;
        this.nbt = nbt;

        this.includePreviousStation = nbt.contains(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) ? nbt.getBoolean(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) : false;
        this.usable = nbt.contains(TravelSectionInstruction.NBT_USABLE) ? nbt.getBoolean(TravelSectionInstruction.NBT_USABLE) : true;
        this.trainGroupId = nbt.contains(TravelSectionInstruction.NBT_TRAIN_GROUP) ? nbt.getString(TravelSectionInstruction.NBT_TRAIN_GROUP) : null;
        this.trainLineId = nbt.contains(TravelSectionInstruction.NBT_TRAIN_LINE) ? nbt.getString(TravelSectionInstruction.NBT_TRAIN_LINE) : null;
    }    

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        nbt.putString(TravelSectionInstruction.NBT_TRAIN_GROUP, trainGroupId == null ? "" : trainGroupId);
        nbt.putString(TravelSectionInstruction.NBT_TRAIN_LINE, trainLineId == null ? "" : trainLineId);
        nbt.putBoolean(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION, includePreviousStation);
        nbt.putBoolean(TravelSectionInstruction.NBT_USABLE, usable);
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = width() / 2 - GUI_WIDTH / 2;
        guiTop = height() / 2 - GUI_HEIGHT / 2;
        
        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 179, guiTop + 119, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });        
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 179 - DEFAULT_ICON_BUTTON_WIDTH - 10, guiTop + 119, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_SCHEDULE_SECTIONS);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            final Screen instance = this;
            globalSettingsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + 119, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    DLScreen.setScreen(new GlobalSettingsScreen(instance));
                }
            });
            addTooltip(DLTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));
        }

        GlobalSettingsClient.getTrainGroups((trainGroups) -> {
            this.groupsById = trainGroups.stream().collect(Collectors.toMap(x -> x.getGroupName(), x -> x));
            GlobalSettingsClient.getTrainLines((trainLines) -> {
                this.linesById = trainLines.stream().collect(Collectors.toMap(x -> x.getLineName(), x -> x));

                List<MutableComponent> groupsList = new ArrayList<>(trainGroups.stream().map(x -> TextUtils.text(x.getGroupName())).toList());
                groupsList.add(0, textNone);
                displayTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
                displayTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 23, 138, 18)
                    .forOptions(groupsList)
                    .titled(tooltipTrainGroup)
                    .writingTo(displayTypeLabel)
                    .calling((i) -> {
                        this.trainGroupId = i <= 0 ? null : trainGroups.get(i - 1).getGroupName();
                    })
                    .setState(trainGroupId != null && groupsById.containsKey(trainGroupId) ? trainGroups.indexOf(groupsById.get(trainGroupId)) + 1 : 0)
                );
                displayTypeInput.onChanged();

                List<MutableComponent> linesList = new ArrayList<>(trainLines.stream().map(x -> TextUtils.text(x.getLineName())).toList());
                linesList.add(0, textNone);
                infoTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
                infoTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 45, 138, 18)
                    .forOptions(linesList)
                    .titled(tooltipTrainLine)
                    .writingTo(infoTypeLabel)
                    .calling((i) -> {
                        this.trainLineId = i <= 0 ? null : trainLines.get(i - 1).getLineName();
                    })
                    .setState(trainLineId != null && linesById.containsKey(trainLineId) ? trainLines.indexOf(linesById.get(trainLineId)) + 1 : 0)
                );
                infoTypeInput.onChanged();  

                addRenderableWidget(new DLCheckBox(guiLeft + 21, guiTop + 67 + 1, 165, textIncludePreviousStation.getString(), includePreviousStation, (box) -> {
                    this.includePreviousStation = box.isChecked();
                }));
                addRenderableWidget(new DLCheckBox(guiLeft + 21, guiTop + 87 + 1, 165, textUsable.getString(), usable, (box) -> {
                    this.usable = box.isChecked();                    
                }));
            });
        });

    }

    @Override
    public void tick() {        
        super.tick();
        DLUtils.doIfNotNull(displayTypeInput, x -> x.tick());
        DLUtils.doIfNotNull(infoTypeInput, x -> x.tick());
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderScreenBackground(graphics);
        GuiUtils.drawTexture(TEXTURE, graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0, 0, 256, 256);
        GuiUtils.drawString(graphics, font, guiLeft + 6, guiTop + 4, getTitle(), DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);

        ModGuiIcons.TRAIN.render(graphics, guiLeft + 22, guiTop + 24);
        ModGuiIcons.MAP_PATH.render(graphics, guiLeft + 22, guiTop + 46);

        GuiGameElement.of(DISPLAY_ITEM).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT - 48, -200)
			.scale(4f)
			.render(graphics.graphics());

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }    

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTick);
        for (Renderable widget : renderables) {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isHoveredOrFocused() && simiWidget.visible) {
                List<Component> tooltip = simiWidget.getToolTip();
                if (tooltip.isEmpty())
                    continue;
                int ttx = simiWidget.lockedTooltipX == -1 ? pMouseX : simiWidget.lockedTooltipX + simiWidget.getX();
                int tty = simiWidget.lockedTooltipY == -1 ? pMouseY : simiWidget.lockedTooltipY + simiWidget.getY();
                graphics.graphics().renderComponentTooltip(font, tooltip, ttx, tty);
            }
        }
    }
}
