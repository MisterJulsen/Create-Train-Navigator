package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    private UUID trainCategoryId;
    private UUID trainLineId;

    private Map<UUID, TrainCategory> categoriesById;
    private Map<UUID, TrainLine> linesById;

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
    private final MutableComponent tooltipTrainCatrgory = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_categories");
    private final MutableComponent tooltipTrainLine = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_lines");
    private final MutableComponent textIncludePreviousStation = TextUtils.translate("gui.createrailwaysnavigator.section_settings.include_previous_station");
    private final MutableComponent textUsable = TextUtils.translate("gui.createrailwaysnavigator.section_settings.usable");
    private final MutableComponent textNone = TextUtils.translate("gui.createrailwaysnavigator.section_settings.none");

    @SuppressWarnings("deprecation")
    public TrainSectionSettingsScreen(Screen lastScreen, CompoundTag nbt) {
        super(TextUtils.translate("gui.createrailwaysnavigator.section_settings.title"));
        this.lastScreen = lastScreen;
        this.nbt = nbt;

        this.includePreviousStation = nbt.contains(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) ? nbt.getBoolean(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) : false;
        this.usable = nbt.contains(TravelSectionInstruction.NBT_USABLE) ? nbt.getBoolean(TravelSectionInstruction.NBT_USABLE) : true;
        
        if (nbt.contains(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY)) {
            this.trainCategoryId = nbt.getTagType(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY) == Tag.TAG_STRING ? TrainCategory.genMD5Uuid(nbt.getString(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY)) : nbt.getUUID(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY);
        } else if (nbt.contains(TravelSectionInstruction.NBT_TRAIN_CATEGORY)) {
            this.trainCategoryId = nbt.getTagType(TravelSectionInstruction.NBT_TRAIN_CATEGORY) == Tag.TAG_STRING ? TrainCategory.genMD5Uuid(nbt.getString(TravelSectionInstruction.NBT_TRAIN_CATEGORY)) : nbt.getUUID(TravelSectionInstruction.NBT_TRAIN_CATEGORY);
        }
        
        if (nbt.contains(TravelSectionInstruction.NBT_TRAIN_LINE)) {
            this.trainLineId = nbt.getTagType(TravelSectionInstruction.NBT_TRAIN_LINE) == Tag.TAG_STRING ? TrainLine.genMD5Uuid(nbt.getString(TravelSectionInstruction.NBT_TRAIN_LINE)) : nbt.getUUID(TravelSectionInstruction.NBT_TRAIN_LINE);
        }
    }    

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (trainCategoryId != null) {
            nbt.putUUID(TravelSectionInstruction.NBT_TRAIN_CATEGORY, trainCategoryId);
        } else {
            nbt.remove(TravelSectionInstruction.NBT_TRAIN_CATEGORY);
        }
        if (trainLineId != null) {
            nbt.putUUID(TravelSectionInstruction.NBT_TRAIN_LINE, trainLineId);
        } else {
            nbt.remove(TravelSectionInstruction.NBT_TRAIN_LINE);
        }
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
        final Screen instance = this;
        globalSettingsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + 119, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                DLScreen.setScreen(new GlobalSettingsScreen(instance));
            }
        });
        addTooltip(DLTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));

        GlobalSettingsClient.getTrainCategories((trainCategories) -> {
            List<TrainCategory> orderedCategories = trainCategories.stream().sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName())).toList();
            this.categoriesById = orderedCategories.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
            GlobalSettingsClient.getTrainLines((trainLines) -> {
                List<TrainLine> orderedLines = trainLines.stream().sorted((a, b) -> a.getLineName().compareToIgnoreCase(b.getLineName())).toList();
                this.linesById = orderedLines.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

                List<MutableComponent> categoriesList = new ArrayList<>(orderedCategories.stream().map(x -> TextUtils.text(x.getCategoryName())).toList());
                categoriesList.add(0, textNone);
                displayTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
                displayTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(this, guiLeft + 45, guiTop + 23, 138, 18)
                    .forOptions(categoriesList)
                    .titled(tooltipTrainCatrgory)
                    .writingTo(displayTypeLabel)
                    .calling((i) -> {
                        this.trainCategoryId = i <= 0 ? null : orderedCategories.get(i - 1).getId();
                    })
                    .setState(trainCategoryId != null && categoriesById.containsKey(trainCategoryId) ? orderedCategories.indexOf(categoriesById.get(trainCategoryId)) + 1 : 0)
                );
                displayTypeInput.onChanged();

                List<MutableComponent> linesList = new ArrayList<>(orderedLines.stream().map(x -> TextUtils.text(x.getLineName())).toList());
                linesList.add(0, textNone);
                infoTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
                infoTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(this, guiLeft + 45, guiTop + 45, 138, 18)
                    .forOptions(linesList)
                    .titled(tooltipTrainLine)
                    .writingTo(infoTypeLabel)
                    .calling((i) -> {
                        this.trainLineId = i <= 0 ? null : orderedLines.get(i - 1).getId();
                    })
                    .setState(trainLineId != null && linesById.containsKey(trainLineId) ? orderedLines.indexOf(linesById.get(trainLineId)) + 1 : 0)
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
        GuiUtils.drawString(graphics, font, guiLeft + 6, guiTop + 4, getTitle(), DragonLib.NATIVE_UI_FONT_COLOR, ETextAlignment.LEFT, false);

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
