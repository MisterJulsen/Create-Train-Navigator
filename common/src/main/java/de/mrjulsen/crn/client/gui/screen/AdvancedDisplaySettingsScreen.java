package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;
import java.util.List;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.AdvancedDisplaySettingsData;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.block.properties.ESide;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.client.gui.widgets.modular.ModularWidgetContainer;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.data.Clipboard;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AdvancedDisplaySettingsScreen extends DLScreen {

    private static boolean advancedSettingsExpanded = false;

    private static final MutableComponent title = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.title");
    private static final int GUI_WIDTH = 212;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;
    private static final int BASIC_GUI_HEIGHT = headerSize.size() + footerSize.size() + 76 + 5 + DLIconButton.DEFAULT_BUTTON_HEIGHT;
    private static final int EXTENDED_GUI_HEIGHT = 220;

    private static final int guiHeight() {
        return advancedSettingsExpanded ? EXTENDED_GUI_HEIGHT : BASIC_GUI_HEIGHT;
    }

    private final Font shadowlessFont;
	private final ItemStack renderedItem;

    // Settings
    private final Level level;
    private final BlockPos pos;
    private DisplayTypeResourceKey typeKey;
    private EDisplayType type;
    private IDisplaySettings settings;
    private final boolean canBeDoubleSided;
    private boolean doubleSided;

    private ScrollInput infoTypeInput;
    private ScrollInput displayTypeInput;
    
    private DLCreateIconButton globalSettingsButton;
    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipDisplayType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type");
    private final MutableComponent tooltipInfoType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type");
    private final MutableComponent textDoubleSided = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.double_sided");

    @SuppressWarnings("resource")
    private final MutableComponent textAdvancedSettings(int maxWidth) {
        Font font = Minecraft.getInstance().font;
        MutableComponent comp = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.advanced_settings").withStyle(ChatFormatting.BOLD);
        MutableComponent ellipsisText = TextUtils.text("...").withStyle(ChatFormatting.BOLD);
        final boolean tooWide = font.width(comp) + font.width(ellipsisText) > maxWidth;
        return tooWide ? TextUtils.text(font.substrByWidth(comp, maxWidth - font.width(ellipsisText)).getString() + "...").withStyle(ChatFormatting.BOLD) : comp;
    }

    private int guiLeft, guiTop;
    private GuiAreaDefinition workingArea;

    private DLCreateIconButton backButton;
    private ModularWidgetContainer commonSettingsContainer;
    private ModularWidgetContainer advancedSettingsContainer;

    private final Cache<List<DisplayTypeResourceKey>> displayTypes = new Cache<>(() -> AdvancedDisplaysRegistry.getAllOfTypeAsKey(type), ECachingPriority.ALWAYS);

    private final AdvancedDisplayBlockEntity blockEntity;
    
    @SuppressWarnings("resource")
    public AdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
        this.settings = blockEntity.getSettings();
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.pos = blockEntity.getBlockPos();
        this.level = blockEntity.getLevel();
        this.type = blockEntity.getDisplayType().category();
        this.typeKey = blockEntity.getDisplayType();
        this.renderedItem = new ItemStack(blockEntity.getBlockState().getBlock());
        this.canBeDoubleSided = blockEntity.getBlockState().getBlock() instanceof AbstractAdvancedSidedDisplayBlock;
        this.doubleSided = !canBeDoubleSided || blockEntity.getBlockState().getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH;
    }

    @Override
    public void onClose() {
        CreateRailwaysNavigator.net().CHANNEL.sendToServer(new AdvancedDisplayUpdatePacket(level, pos, typeKey, doubleSided, settings));
        super.onClose();
    }

    private void reinit() {        
        this.clearWidgets();
        this.setFocused((GuiEventListener)null);
        this.init();
        this.triggerImmediateNarration(false);
    }

    @Override
    protected void init() {
        super.init();
        displayTypeInput = null;
        infoTypeInput = null;

        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - guiHeight() / 2;
        workingArea = new GuiAreaDefinition(guiLeft + 1, guiTop + headerSize.size(), GUI_WIDTH - 2, guiHeight() - headerSize.size() - footerSize.size());

        // Content
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, 0, 0, 0, GuiAreaDefinition.empty());
        commonSettingsContainer = addRenderableWidget(new ModularWidgetContainer(this, workingArea.getX() + 2, workingArea.getY() + 1, workingArea.getWidth() - 4, 76, (w, builder) ->  {
            builder.addLine("type", (line) -> {
                IconSlotWidget icon = line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, type.getIcon().getAsSprite(16, 16)));
                displayTypeInput = line.add(new DLCreateSelectionScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, 18)
                    .setRenderArrow(true)
                    .forOptions(Arrays.stream(EDisplayType.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
                    .setState(type.getId())
                    .titled(tooltipDisplayType)
                    .calling((i) -> {
                        type = EDisplayType.getTypeById(i);
                        icon.setIcon(type.getIcon().getAsSprite(16, 16));
                        displayTypes.clear();
                        displayTypeInput.addHint(TextUtils.translate(type.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID)));

                        DLUtils.doIfNotNull((SelectionScrollInput)infoTypeInput, x -> {
                            x.setState(0);
                            x.forOptions(displayTypes.get().stream().map(a -> TextUtils.translate(a.getTranslationKey())).toList());
                            x.onChanged();
                        });
                    })
                    .addHint(TextUtils.translate(type.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID))));
                displayTypeInput.onChanged();
            });
            builder.addLine("variant", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.VERY_DETAILED.getAsSprite(16, 16)));
                infoTypeInput = line.add(new DLCreateSelectionScrollInput(this, line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, 18)
                    .setRenderArrow(true)
                    .forOptions(displayTypes.get().stream().map(x -> TextUtils.translate(x.getTranslationKey())).toList())
                    .setState(displayTypes.get().indexOf(typeKey)))
                    .titled(tooltipInfoType)
                    .calling((i) -> {
                        typeKey = displayTypes.get().get(i);
                        IDisplaySettings oldSettings = settings;
                        settings = blockEntity.getDisplayType().equals(typeKey) ? blockEntity.getSettings() : AdvancedDisplaysRegistry.createSettings(typeKey);
                        settings.onChangeSettings(oldSettings);
                        DLUtils.doIfNotNull(advancedSettingsContainer, ModularWidgetContainer::build);
                    })
                ;
                infoTypeInput.onChanged();
            });
            builder.addLine("double_sided", (line) -> {
                line.add(new IconSlotWidget(line.getCurrentX(), line.y() + 2, ModGuiIcons.DOUBLE_SIDED.getAsSprite(16, 16)));
                line.add(new DLCheckBox(line.getCurrentX() + 6, line.getY() + 2, line.getRemainingWidth() - 6, textDoubleSided.getString(), doubleSided, (box) -> {
                    this.doubleSided = box.isChecked();
                })).active = canBeDoubleSided;
            });
        }, scrollBar, 18, 18, 4, 4));        
        addRenderableWidget(scrollBar);

        // Advanced Settings
        final DLScreen screen = this;
        DLIconButton copyBtn = null;
        DLIconButton pasteBtn = null;
        DLIconButton resetBtn = null;

        if (advancedSettingsExpanded) {
            ModernVerticalScrollBar advancedSettingsScrollBar = new ModernVerticalScrollBar(this, 0, 0, 0, GuiAreaDefinition.empty());
            advancedSettingsContainer = addRenderableWidget(new ModularWidgetContainer(this, workingArea.getX() + 2, commonSettingsContainer.y() + commonSettingsContainer.height() + 3 + 18, workingArea.getWidth() - 4, workingArea.getHeight() - 2 - commonSettingsContainer.height() - 3 - 18, (w, builder) ->  {
                settings.buildGui(new GuiBuilderContext(builder, w));
            }, advancedSettingsScrollBar, 18, 18, 0, 4));    
            advancedSettingsScrollBar.setScrollArea(GuiAreaDefinition.of(advancedSettingsContainer));
            addRenderableWidget(advancedSettingsScrollBar);
    
            copyBtn = addRenderableWidget(new DLIconButton(
                ButtonType.DEFAULT,
                AreaStyle.FLAT,
                ModGuiIcons.COPY.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE),
                workingArea.getX() + workingArea.getWidth() - 2 - DLIconButton.DEFAULT_BUTTON_WIDTH,
                commonSettingsContainer.y() + commonSettingsContainer.height() + 3,
                TextUtils.empty(),
                (b) -> {
                    Clipboard.put(AdvancedDisplaySettingsData.class, new AdvancedDisplaySettingsData(typeKey, settings, doubleSided));
                }
            ) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(screen, this, List.of(Constants.TEXT_COPY), screen.width() / 3, graphics, mouseX, mouseY);
                }
            });
            copyBtn.setBackColor(0);
            pasteBtn = addRenderableWidget(new DLIconButton(
                ButtonType.DEFAULT,
                AreaStyle.FLAT,
                ModGuiIcons.PASTE.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE),
                copyBtn.x() - DLIconButton.DEFAULT_BUTTON_WIDTH,
                copyBtn.y(),
                TextUtils.empty(),
                (b) -> {
                    Clipboard.get(AdvancedDisplaySettingsData.class).ifPresent(x -> {
                        this.typeKey = x.getKey();
                        this.type = x.getKey().category();
                        this.settings = x.getSettings();
                        this.doubleSided = x.isDoubleSided();
                        reinit();
                    });
                }
            ) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(screen, this, List.of(Constants.TEXT_PASTE), screen.width() / 3, graphics, mouseX, mouseY);
                }
            });
            pasteBtn.setBackColor(0);
            resetBtn = addRenderableWidget(new DLIconButton(
                ButtonType.DEFAULT,
                AreaStyle.FLAT,
                ModGuiIcons.REFRESH.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE),
                pasteBtn.x() - DLIconButton.DEFAULT_BUTTON_WIDTH,
                pasteBtn.y(),
                TextUtils.empty(),
                (b) -> {
                    settings = AdvancedDisplaysRegistry.createSettings(typeKey);
                    DLUtils.doIfNotNull(advancedSettingsContainer, ModularWidgetContainer::build);
                }
            ) {
                @Override
                public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);                    
                    if (!isMouseSelected()) {
                        return;
                    }
                    GuiUtils.renderTooltip(screen, this, List.of(Constants.TEXT_RESET), screen.width() / 3, graphics, mouseX, mouseY);
                }
            });
            resetBtn.setBackColor(0);
        }

        DLIconButton expandCollapseBtn = addRenderableWidget(new DLIconButton(
            ButtonType.DEFAULT,
            AreaStyle.FLAT,
            (advancedSettingsExpanded ? GuiIcons.ARROW_DOWN : GuiIcons.ARROW_RIGHT).getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE),
            workingArea.getX() + 2,
            commonSettingsContainer.y() + commonSettingsContainer.height() + 3,
            workingArea.getWidth() - 4 - (advancedSettingsExpanded ? copyBtn.width() + pasteBtn.width() + resetBtn.width() : 0),
            DLIconButton.DEFAULT_BUTTON_HEIGHT,
            TextUtils.empty(),
            (b) -> {
                advancedSettingsExpanded = !advancedSettingsExpanded;
                reinit();
            }
        ));
        expandCollapseBtn.setMessage(textAdvancedSettings(expandCollapseBtn.width() - ModGuiIcons.ICON_SIZE - 6));
        expandCollapseBtn.setTextAlignment(EAlignment.LEFT);
        expandCollapseBtn.setBackColor(0);
        expandCollapseBtn.setFontColor(DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE);
        
        // Buttons
        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 7 - DEFAULT_ICON_BUTTON_WIDTH, guiTop + guiHeight() - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 17 - DEFAULT_ICON_BUTTON_WIDTH * 2, guiTop + guiHeight() - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_ADVANCED_DISPLAYS);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            final Screen instance = this;
            globalSettingsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + guiHeight() - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    DLScreen.setScreen(new GlobalSettingsScreen(instance));
                }
            });
            addTooltip(DLTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderScreenBackground(graphics);
        CreateDynamicWidgets.renderWindow(graphics, guiLeft, guiTop, GUI_WIDTH, guiHeight(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), false);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, guiLeft + GUI_WIDTH - 31, guiTop + guiHeight() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);
        GuiUtils.drawTexture(CRNGui.GUI, graphics, guiLeft + GUI_WIDTH - 3, guiTop + guiHeight() - footerSize.size() / 2 - 9, 11, 18, 0, 12, 11, 18, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT);

        int commonHeight = commonSettingsContainer.getHeight() + 4;
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX(), workingArea.getY() - 1, workingArea.getWidth(), commonHeight, ContainerColor.PURPLE);
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX(), workingArea.getY() - 2 + commonHeight, workingArea.getWidth(), workingArea.getHeight() - commonHeight + 3, ContainerColor.GRAY);
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 6, guiTop + 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);

        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH + 11, guiTop + guiHeight() - 48, -200)
			.scale(4f)
			.render(graphics.poseStack());
    }
}