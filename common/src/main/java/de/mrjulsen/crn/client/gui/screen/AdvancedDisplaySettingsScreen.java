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

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateLabel;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    private EDisplayInfo info;
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
    private final MutableComponent tooltipDisplayTypeDescription = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type.description");
    private final MutableComponent tooltipInfoType = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type");
    private final MutableComponent tooltipInfoTypeDescription = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type.description");
    private final MutableComponent textDoubleSided = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.double_sided");

    private int guiLeft, guiTop;

    private DLCreateIconButton backButton;
    
    @SuppressWarnings("resource")
    public AdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        super(title);
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.pos = blockEntity.getBlockPos();
        this.level = blockEntity.getLevel();
        this.info = blockEntity.getInfoType();
        this.type = blockEntity.getDisplayType();
        this.renderedItem = new ItemStack(blockEntity.getBlockState().getBlock());
        this.canBeDoubleSided = blockEntity.getBlockState().getBlock() instanceof AbstractAdvancedSidedDisplayBlock;
        this.doubleSided = !canBeDoubleSided || blockEntity.getBlockState().getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH;
    }

    @Override
    public void onClose() {        
        CreateRailwaysNavigator.net().CHANNEL.sendToServer(new AdvancedDisplayUpdatePacket(level, pos, type, info, doubleSided));
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

        displayTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
        displayTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 23, 138, 18)
            .forOptions(Arrays.stream(EDisplayType.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
            .titled(tooltipDisplayType)
            .writingTo(displayTypeLabel)
            .calling((i) -> {
                type = EDisplayType.getTypeById(i);
            })
            .addHint(tooltipDisplayTypeDescription)
            .setState(type.getId()));
        displayTypeInput.onChanged();

        infoTypeLabel = addRenderableWidget(new DLCreateLabel(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
        infoTypeInput = addRenderableWidget(new DLCreateSelectionScrollInput(guiLeft + 45, guiTop + 45, 138, 18)
            .forOptions(Arrays.stream(EDisplayInfo.values()).map(x -> TextUtils.translate(x.getValueTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
            .titled(tooltipInfoType)
            .writingTo(infoTypeLabel)
            .calling((i) -> {
                info = EDisplayInfo.getTypeById(i);
            })
            .addHint(tooltipInfoTypeDescription)
            .setState(info.getId()));
        infoTypeInput.onChanged();
        

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
                    minecraft.setScreen(new LoadingScreen());
                    GlobalSettingsManager.syncToClient(() -> {
                        ClientTrainStationSnapshot.syncToClient(() -> {
                            minecraft.setScreen(new GlobalSettingsScreen(level, instance));
                        });
                    });
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
        info.getIcon().render(graphics, guiLeft + 22, guiTop + 46);
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