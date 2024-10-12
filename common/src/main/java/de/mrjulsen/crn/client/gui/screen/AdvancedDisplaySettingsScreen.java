package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.block.properties.ESide;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateLabel;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AdvancedDisplaySettingsScreen extends DLScreen {

    private static final MutableComponent title = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.title");
    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/advanced_display_settings.png");
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 123;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private final Font shadowlessFont;
	private final ItemStack renderedItem;

    // Settings
    private final Level level;
    private final BlockPos pos;
    private DisplayTypeResourceKey typeKey;
    private EDisplayType type;
    private final boolean canBeDoubleSided;
    private boolean doubleSided;

    private ScrollInput infoTypeInput;
    private Label infoTypeLabel;
    private ScrollInput displayTypeInput;
    private Label displayTypeLabel;
    
    private DLCreateIconButton globalSettingsButton;
    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipDisplayType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type");
    private final MutableComponent tooltipInfoType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type");
    private final MutableComponent textDoubleSided = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.double_sided");

    private int guiLeft, guiTop;

    private DLCreateIconButton backButton;

    private final Cache<List<DisplayTypeResourceKey>> displayTypes = new Cache<>(() -> AdvancedDisplaysRegistry.getAllOfTypeAsKey(type));
    
    @SuppressWarnings("resource")
    public AdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        super(title);
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.pos = blockEntity.getBlockPos();
        this.level = blockEntity.getLevel();
        this.type = blockEntity.getDisplayTypeKey().category();
        this.typeKey = blockEntity.getDisplayTypeKey();
        this.renderedItem = new ItemStack(blockEntity.getBlockState().getBlock());
        this.canBeDoubleSided = blockEntity.getBlockState().getBlock() instanceof AbstractAdvancedSidedDisplayBlock;
        this.doubleSided = !canBeDoubleSided || blockEntity.getBlockState().getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH;
    }

    @Override
    public void onClose() {        
        CreateRailwaysNavigator.net().CHANNEL.sendToServer(new AdvancedDisplayUpdatePacket(level, pos, typeKey, doubleSided));
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 179, guiTop + 99, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 179 - DEFAULT_ICON_BUTTON_WIDTH - 10, guiTop + 99, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_ADVANCED_DISPLAYS);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        displayTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
        displayTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 23, 138, 18)
            .forOptions(Arrays.stream(EDisplayType.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
            .titled(tooltipDisplayType)
            .writingTo(displayTypeLabel)
            .calling((i) -> {
                type = EDisplayType.getTypeById(i);
                displayTypes.clear();
                createDisplayBrowser();
                displayTypeInput.addHint(displayTypeHint());
            })
            .addHint(displayTypeHint())
            .setState(type.getId()));
        displayTypeInput.onChanged();

        infoTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
        createDisplayBrowser();        

        addRenderableWidget(new DLCheckBox(guiLeft + 45, guiTop + 67 + 1, 138, textDoubleSided.getString(), doubleSided, (box) -> {
            this.doubleSided = box.isChecked();
        })).active = canBeDoubleSided;

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            final Screen instance = this;
            globalSettingsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + 99, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    DLScreen.setScreen(new GlobalSettingsScreen(instance));
                }
            });
            addTooltip(DLTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));
        }
    }

    private void createDisplayBrowser() {
        if (infoTypeInput != null) {
            removeWidget(infoTypeInput);
        }
        infoTypeInput = new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 45, 138, 18)
            .forOptions(displayTypes.get().stream().map(x -> TextUtils.translate(x.getTranslationKey())).toList())
            .titled(tooltipInfoType)
            .writingTo(infoTypeLabel)
            .calling((i) -> {
                typeKey = displayTypes.get().get(i);
            })
            .setState(displayTypes.get().indexOf(typeKey));
        infoTypeInput.onChanged();
        addRenderableWidget(infoTypeInput);
    }

    private MutableComponent displayTypeHint() {
        StringBuilder sb = new StringBuilder();
        font.getSplitter().splitLines(TextUtils.translate(typeKey.category().getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID)), width() / 3, Style.EMPTY).forEach(x -> {
            sb.append("\n" + x.getString());
        });
        return TextUtils.text(sb.toString());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        infoTypeInput.tick();
        displayTypeInput.tick();
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 6, guiTop + 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);
        
        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT - 48, -200)
			.scale(4f)
			.render(graphics.graphics());

        type.getIcon().render(graphics, guiLeft + 22, guiTop + 24);
        ModGuiIcons.VERY_DETAILED.render(graphics, guiLeft + 22, guiTop + 46);
        ModGuiIcons.DOUBLE_SIDED.render(graphics, guiLeft + 22, guiTop + 68);            

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTick);
        for (Renderable widget : renderables) {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isHoveredOrFocused()
                && simiWidget.visible) {
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