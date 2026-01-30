package de.mrjulsen.crn.client.gui.windows;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.gui.AllIcons;
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
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.FlyoutConfirmDialog;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.ModularWidgetContainer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCycleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLToggleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.Clipboard;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplaySettingsWindow extends DLWindow {

    private static boolean advancedSettingsExpanded = false;

    private static final MutableComponent title = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.title");
    private static final int GUI_WIDTH = 212;
    
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;
    private static final int BASIC_GUI_HEIGHT = headerSize.size() + footerSize.size() + 76 + 5 + CreateButton.HEIGHT;

	private final ItemStack renderedItem;

    // Settings
    private final AbstractContraptionEntity contraption;
    private final Level level;
    private final BlockPos pos;
    private final boolean canBeDoubleSided;

    private DisplayTypeResourceKey typeKey;
    private EDisplayType type;
    private IDisplaySettings settings;
    private boolean doubleSided;
    
    private CreateButton globalSettingsButton;
    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipDisplayType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type");
    private final MutableComponent tooltipInfoType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type");
    private final MutableComponent textDoubleSided = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.double_sided");

    private final MutableComponent textAdvancedSettings(int maxWidth) {
        Font font = Minecraft.getInstance().font;
        MutableComponent comp = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.advanced_settings").withStyle(ChatFormatting.BOLD);
        MutableComponent ellipsisText = TextUtils.text("...").withStyle(ChatFormatting.BOLD);
        final boolean tooWide = font.width(comp) + font.width(ellipsisText) > maxWidth;
        return tooWide ? TextUtils.text(font.substrByWidth(comp, maxWidth - font.width(ellipsisText)).getString() + "...").withStyle(ChatFormatting.BOLD) : comp;
    }

    private Rectangle workingArea;

    private CreateButton backButton;
    private ModularWidgetContainer commonSettingsContainer;
    private DLPanel advancedSettingsPanel;
    private ModularWidgetContainer advancedSettingsContainer;

    private final Cache<List<DisplayTypeResourceKey>> displayTypes = new Cache<>(() -> AdvancedDisplaysRegistry.getAllOfTypeAsKey(type), ECachingPriority.ALWAYS);

    private final AdvancedDisplayBlockEntity blockEntity;
    
    public AdvancedDisplaySettingsWindow(DLWindowManager manager, AdvancedDisplayBlockEntity blockEntity, AbstractContraptionEntity contraption) {
        super(manager);
        setSize(GUI_WIDTH, BASIC_GUI_HEIGHT);
        windowSpawnPosition.set(WindowPosition.CENTER);

        this.blockEntity = blockEntity;
        this.settings = blockEntity.getSettings();
        this.pos = blockEntity.getBlockPos();
        boolean isOnContraption = contraption != null;
        this.contraption = contraption;
        this.level = isOnContraption ? contraption.getContraption().getContraptionWorld() : blockEntity.getLevel();
        this.type = blockEntity.getDisplayType().category();
        this.typeKey = blockEntity.getDisplayType();
        BlockState state = level.getBlockState(blockEntity.getBlockPos());
        this.renderedItem = new ItemStack(state.getBlock());
        this.canBeDoubleSided = state.getBlock() instanceof AbstractAdvancedSidedDisplayBlock;
        this.doubleSided = !canBeDoubleSided || state.getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH;

        init();
    }

    @Override
    public void close() {
        ModNetworkManager.ADVANCED_DISPLAY_UPDATE_PACKET.send(NetworkDirection.toServer(), new AdvancedDisplayUpdatePacketData(level, pos, contraption, typeKey, doubleSided, settings));
    }

    private void reinit() {
        setHeight(headerSize.size() + footerSize.size() + commonSettingsContainer.height() + 2 + advancedSettingsPanel.height() + 3 + (advancedSettingsExpanded ? advancedSettingsContainer.height() : 0));
        setPosition(getWindowManager().getScreenWidth() / 2 - width() / 2, getWindowManager().getScreenHeight() / 2 - height() / 2);
        advancedSettingsContainer.clearLines();
        advancedSettingsContainer.visible.set(advancedSettingsExpanded);
        if (advancedSettingsExpanded) {
            settings.buildGui(new GuiBuilderContext(advancedSettingsContainer));
        }
    }

    protected void init() {        
        backButton = addComponent(new CreateButton(GUI_WIDTH - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        
        CreateButton helpButton = addComponent(new CreateButton(GUI_WIDTH - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_ADVANCED_DISPLAYS);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));

        globalSettingsButton = addComponent(new CreateButton(7, height() - 6 - CreateButton.HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()));
        globalSettingsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new GlobalSettingsWindow(mgr));
            return false;
        });
        globalSettingsButton.tooltip.set(new DLTooltip(List.of(tooltipGlobalSettings), 200));

        //ModularWidgetContainer commonSettingsContainer = addComponent(new ModularWidgetContainer(3, headerSize.size()))
        //workingArea = new GuiAreaDefinition(1, headerSize.size(), width() - 2, guiHeight() - headerSize.size() - footerSize.size());

        // Content
        commonSettingsContainer = addComponent(new ModularWidgetContainer(3, headerSize.size() + 1, width() - 6, 1));
        if (commonSettingsContainer.contentPanel.layout.get() instanceof FlowLayout fl) {
            fl.padding.set(new Padding(6, 16, 6, 16));
        }
        commonSettingsContainer.addEventListener(ModularWidgetContainer.ContentLayoutUpdatedEvent.class, (s, e) -> {
            commonSettingsContainer.setHeight(e.layoutResult().contentHeight());
            return false;
        });
        
        DLPanel displayTypeLine = commonSettingsContainer.addLine("displayType");
        IconSlotWidget displayTypeIcon = displayTypeLine.addComponent(new IconSlotWidget(0, 0));
        displayTypeIcon.icon.set(type.getIcon().getAsSprite(16, 16));

        CreateItemPicker<EDisplayType> displayTypePicker = displayTypeLine.addComponent(new CreateItemPicker<>(0, 0, 100));
        displayTypePicker.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        displayTypePicker.renderArrow.set(true);
        displayTypePicker.hint.set(EDisplayType.DEPARTURE_BOARD.getEnumDescriptionTranslation());
        displayTypePicker.title.set(tooltipDisplayType);
        displayTypePicker.items.addAll(EDisplayType.values());
        displayTypePicker.selectedItem.set(Optional.ofNullable(type));
        displayTypePicker.layoutContraint.set(FlowLayout.FlowConstraint.FILL);


        DLPanel displayVariantLine = commonSettingsContainer.addLine("displayVariant");
        IconSlotWidget displayVariantIcon = displayVariantLine.addComponent(new IconSlotWidget(0, 0));
        displayVariantIcon.icon.set(ModGuiIcons.VERY_DETAILED.getAsSprite(16, 16));

        CreateItemPicker<DisplayTypeResourceKey> displayVariantPicker = displayVariantLine.addComponent(new CreateItemPicker<>(0, 0, 100));
        displayVariantPicker.formatter.set(item -> item == null ? TextUtils.empty() : TextUtils.translate(item.getTranslationKey()));
        displayVariantPicker.renderArrow.set(true);
        displayVariantPicker.hint.set(EDisplayType.DEPARTURE_BOARD.getEnumDescriptionTranslation());
        displayVariantPicker.title.set(tooltipDisplayType);
        displayVariantPicker.items.addAll(displayTypes.get());
        displayVariantPicker.selectedItem.set(Optional.ofNullable(typeKey));
        displayVariantPicker.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        
        displayTypePicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            displayTypePicker.selectedItem.get().ifPresent(item -> type = item);
            displayTypeIcon.icon.set(type.getIcon().getAsSprite(16, 16));
            displayTypes.clear();
            
            displayVariantPicker.items.set(displayTypes.get());
            displayVariantPicker.selectedItem.set(Optional.ofNullable(typeKey));

            /*            
            type = EDisplayType.getTypeById(i);
            displayTypes.clear();
            displayTypeInput.addHint(TextUtils.translate(type.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID)));

            DLUtils.doIfNotNull((SelectionScrollInput)infoTypeInput, x -> {
                x.setState(0);
                x.forOptions(displayTypes.get().stream().map(a -> TextUtils.translate(a.getTranslationKey())).toList());
                x.onChanged();
            }); */
            return false;
        });
        displayVariantPicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            if (e.index() < 0) {
                return false;
            }
            typeKey = displayTypes.get().get(e.index());
            IDisplaySettings oldSettings = settings;
            settings = blockEntity.getDisplayType().equals(typeKey) ? blockEntity.getSettings() : AdvancedDisplaysRegistry.createSettings(typeKey);
            settings.onChangeSettings(oldSettings);
            reinit();
            return false;
        });

        
        DLPanel doubleSidedLine = commonSettingsContainer.addLine("doubleSided");
        IconSlotWidget doubleSidedIcon = doubleSidedLine.addComponent(new IconSlotWidget(0, 0));
        doubleSidedIcon.icon.set(ModGuiIcons.DOUBLE_SIDED.getAsSprite(16, 16));

        DLCheckBox doubleSidedBox = doubleSidedLine.addComponent(new DLCheckBox(0, 0, 100, CreateButton.HEIGHT));
        doubleSidedBox.enabled.set(canBeDoubleSided);
        doubleSidedBox.checked.set(doubleSided);
        doubleSidedBox.text.set(textDoubleSided);
        doubleSidedBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        doubleSidedBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            this.doubleSided = e.checked();
            return false;
        });


        // Advanced Settings
        advancedSettingsPanel = addComponent(new DLPanel(commonSettingsContainer.x(), commonSettingsContainer.y() + commonSettingsContainer.height() + 3, commonSettingsContainer.width(), CreateButton.HEIGHT));
        FlowLayout advancedSettingsPanelLayout = new FlowLayout();
        advancedSettingsPanelLayout.flowDirection.set(Direction.HORIZONTAL);
        advancedSettingsPanel.layout.set(advancedSettingsPanelLayout);

        FlatIconButton copyBtn = advancedSettingsPanel.addComponent(new FlatIconButton(0, 0, ModGuiIcons.COPY.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        copyBtn.layoutContraint.set(FlowLayout.FlowConstraint.END);
        copyBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Clipboard.put(AdvancedDisplaySettingsData.class, new AdvancedDisplaySettingsData(typeKey, settings, doubleSided));
            return false;
        });
        copyBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_COPY), 200));
        
        FlatIconButton pasteBtn = advancedSettingsPanel.addComponent(new FlatIconButton(0, 0, ModGuiIcons.PASTE.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        pasteBtn.layoutContraint.set(FlowLayout.FlowConstraint.END);
        pasteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Clipboard.get(AdvancedDisplaySettingsData.class).ifPresent(x -> {
                this.typeKey = x.getKey();
                this.type = x.getKey().category();
                this.settings = x.getSettings();
                this.doubleSided = x.isDoubleSided();
                reinit();
            });
            return false;
        });
        pasteBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_PASTE), 200));
        
        FlatIconButton resetBtn = advancedSettingsPanel.addComponent(new FlatIconButton(0, 0, ModGuiIcons.REFRESH.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        resetBtn.layoutContraint.set(FlowLayout.FlowConstraint.END);
        resetBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, resetBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                settings = AdvancedDisplaysRegistry.createSettings(typeKey);
                reinit();
            }));
            return false;
        });
        resetBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_RESET), 200));
        
        
        DLButton expandBtn = advancedSettingsPanel.addComponent(new DLButton(0, 0, 0, CreateButton.HEIGHT));
        expandBtn.icon.set((advancedSettingsExpanded ? GuiIcons.ARROW_DOWN : GuiIcons.ARROW_RIGHT).getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE));
        expandBtn.text.set(textAdvancedSettings(200));
        expandBtn.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        expandBtn.textAlignment.set(ETextAlignment.LEFT);
        expandBtn.iconAlignment.set(ETextAlignment.LEFT);
        expandBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        expandBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            advancedSettingsExpanded = !advancedSettingsExpanded;
            reinit();
            return false;
        });


        // Advanced Settings section        
        advancedSettingsContainer = addComponent(new ModularWidgetContainer(advancedSettingsPanel.x(), advancedSettingsPanel.y() + advancedSettingsPanel.height(), advancedSettingsPanel.width(), 100));
        if (advancedSettingsContainer.contentPanel.layout.get() instanceof FlowLayout fl) {
            fl.padding.set(new Padding(2, 16, 6, 16));
        }

        reinit();

        

        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            backButton.setPosition(GUI_WIDTH - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT);
            helpButton.setPosition(GUI_WIDTH - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT);
            globalSettingsButton.setPosition(7, height() - 6 - CreateButton.HEIGHT);
            return false;
        });

    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, GUI_WIDTH, height(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), false);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, GUI_WIDTH - 31, height() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);
        GuiUtils.drawTexture(CRNGui.GUI, graphics, GUI_WIDTH - 3, height() - footerSize.size() / 2 - 9, 11, 18, 0, 12, 11, 18, TextureFillMode.STRETCH);

        CreateDynamicWidgets.renderContainer(graphics, commonSettingsContainer.x() - 2, commonSettingsContainer.y() - 2, commonSettingsContainer.width() + 4, commonSettingsContainer.height() + 4, ContainerColor.PURPLE);
        CreateDynamicWidgets.renderContainer(graphics, advancedSettingsPanel.x() - 2, advancedSettingsPanel.y() - 2, advancedSettingsPanel.width() + 4, advancedSettingsPanel.height() + 4 + (advancedSettingsExpanded ? advancedSettingsContainer.height() : 0), ContainerColor.GRAY);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);

        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(GUI_WIDTH + 11, height() - 48, -200)
			.scale(4f)
			.render(graphics.graphics());
    }
}