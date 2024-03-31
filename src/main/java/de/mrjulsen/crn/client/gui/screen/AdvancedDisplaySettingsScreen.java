package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.Tooltip;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AdvancedDisplaySettingsScreen extends CommonScreen {

    private static final Component title = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.title");
    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/advanced_display_settings.png");
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 123;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private final Font shadowlessFont;
	private final ItemStack renderedItem;

    // Settings
    private final AdvancedDisplayBlockEntity blockEntity;
    private ESide side;

    private ScrollInput infoTypeInput;
    private Label infoTypeLabel;
    private ScrollInput displayTypeInput;
    private Label displayTypeLabel;    
    private ScrollInput sidesInput;
    private Label sidesLabel;
    
    private IconButton globalSettingsButton;
    private final MutableComponent tooltipGlobalSettings = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipDisplayType = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type");
    private final MutableComponent tooltipDisplayTypeDescription = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.display_type.description");
    private final MutableComponent tooltipInfoType = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type");
    private final MutableComponent tooltipInfoTypeDescription = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.info_type.description");
    private final MutableComponent tooltipSides = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.sides");
    private final MutableComponent tooltipSidesDescription = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.sides.description");

    private int guiLeft, guiTop;

    private IconButton backButton;
    
    @SuppressWarnings("resource")
    public AdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        super(title);
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.blockEntity = blockEntity;
        this.side = blockEntity.getBlockState().getValue(AbstractAdvancedDisplayBlock.SIDE);
        this.renderedItem = new ItemStack(blockEntity.getBlockState().getBlock());
    }

    @Override
    public void onClose() {
        NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new AdvancedDisplayUpdatePacket(blockEntity.getBlockPos(), blockEntity, side));
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 179, guiTop + 99, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });

        displayTypeLabel = addRenderableWidget(new Label(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
        displayTypeInput = addRenderableWidget(new SelectionScrollInput(guiLeft + 45, guiTop + 23, 138, 18)
            .forOptions(Arrays.stream(EDisplayType.values()).map(x -> Utils.translate(x.getValueTranslationKey(ModMain.MOD_ID))).toList())
            .titled(tooltipDisplayType)
            .writingTo(displayTypeLabel)
            .calling((i) -> {
                blockEntity.setDisplayType(EDisplayType.getTypeById(i));
            })
            .addHint(tooltipDisplayTypeDescription)
            .setState(blockEntity.getDisplayType().getId()));
        displayTypeInput.onChanged();

        infoTypeLabel = addRenderableWidget(new Label(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
        infoTypeInput = addRenderableWidget(new SelectionScrollInput(guiLeft + 45, guiTop + 45, 138, 18)
            .forOptions(Arrays.stream(EDisplayInfo.values()).map(x -> Utils.translate(x.getValueTranslationKey(ModMain.MOD_ID))).toList())
            .titled(tooltipInfoType)
            .writingTo(infoTypeLabel)
            .calling((i) -> {
                blockEntity.setInfoType(EDisplayInfo.getTypeById(i));
            })
            .addHint(tooltipInfoTypeDescription)
            .setState(blockEntity.getInfoType().getId()));
        infoTypeInput.onChanged();
        
        sidesLabel = addRenderableWidget(new Label(guiLeft + 45 + 5, guiTop + 67 + 5, Components.immutableEmpty()).withShadow());
        sidesInput = addRenderableWidget(new SelectionScrollInput(guiLeft + 45, guiTop + 67, 138, 18)
            .forOptions(Arrays.stream(ESide.values()).map(x -> Utils.translate(x.getValueTranslationKey(ModMain.MOD_ID))).toList())
			.titled(tooltipSides)
            .writingTo(sidesLabel)
            .calling((i) -> {
                blockEntity.applyToAll(be -> {
                    ESide newSide = ESide.getSideById(i);
                    this.side = newSide;
                });
            })
            .addHint(tooltipSidesDescription)
            .setState(side.getId()));
        sidesInput.onChanged();

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            final Screen instance = this;
            globalSettingsButton = this.addRenderableWidget(new IconButton(guiLeft + 7, guiTop + 99, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    minecraft.setScreen(new GlobalSettingsScreen(blockEntity.getLevel(), instance));
                }
            });
            addTooltip(Tooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));
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
        sidesInput.tick();
    }
    
    @Override
    public void renderBg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        GuiUtils.blit(GUI, pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 6, guiTop + 4, DragonLibConstants.DEFAULT_UI_FONT_COLOR);
        
        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT - 48, -200)
			.scale(4f)
			.render(pPoseStack);

        blockEntity.getDisplayType().getIcon().render(pPoseStack, guiLeft + 22, guiTop + 24);
        blockEntity.getInfoType().getIcon().render(pPoseStack, guiLeft + 22, guiTop + 46);
        ModGuiIcons.DOUBLE_SIDED.render(pPoseStack, guiLeft + 22, guiTop + 68);            

        super.renderBg(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderFg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderFg(pPoseStack, pMouseX, pMouseY, pPartialTick);
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
    }
}