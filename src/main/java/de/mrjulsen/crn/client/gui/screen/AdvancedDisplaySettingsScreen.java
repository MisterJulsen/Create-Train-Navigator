package de.mrjulsen.crn.client.gui.screen;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.UpdateBlockEntityPacket;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AdvancedDisplaySettingsScreen extends CommonScreen {

    private static final Component title = Utils.translate("gui.createrailwaysnavigator.advanced_display_settings.title");
    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/advanced_display_settings.png");
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 107;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private final Font shadowlessFont;
	private final ItemStack renderedItem = new ItemStack(ModBlocks.ADVANCED_DISPLAY.get());

    // Settings
    private final AdvancedDisplayBlockEntity blockEntity;

    private ScrollInput infoTypeInput;
    private Label infoTypeLabel;    
    private ScrollInput displayTypeInput;
    private Label displayTypeLabel;    
    private IconButton doubleSidedButton;
    private Indicator doubleSidedIndicator;

    private int guiLeft, guiTop;

    private IconButton backButton;
    
    @SuppressWarnings("resource")
    public AdvancedDisplaySettingsScreen(AdvancedDisplayBlockEntity blockEntity) {
        super(title);
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.blockEntity = blockEntity;
    }

    @Override
    public void onClose() {
        NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new UpdateBlockEntityPacket(blockEntity.getBlockPos(), blockEntity));
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 179, guiTop + 83, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });

        displayTypeLabel = addRenderableWidget(new Label(guiLeft + 45 + 5, guiTop + 23 + 5, Components.immutableEmpty()).withShadow());
        displayTypeInput = addRenderableWidget(new SelectionScrollInput(guiLeft + 45, guiTop + 23, 138, 18)
            .forOptions(Arrays.stream(EDisplayType.values()).map(x -> Utils.translate(x.getValueTranslationKey(ModMain.MOD_ID))).toList())
            .writingTo(displayTypeLabel)
            //.titled(filterSelectionBoxText.copy())
            .calling((i) -> {
                blockEntity.setDisplayType(EDisplayType.getTypeById(i));
            })
            .setState(blockEntity.getDisplayType().getId()));
        displayTypeInput.onChanged();

        infoTypeLabel = addRenderableWidget(new Label(guiLeft + 45 + 5, guiTop + 45 + 5, Components.immutableEmpty()).withShadow());
        infoTypeInput = addRenderableWidget(new SelectionScrollInput(guiLeft + 45, guiTop + 45, 138, 18)
            .forOptions(Arrays.stream(EDisplayInfo.values()).map(x -> Utils.translate(x.getValueTranslationKey(ModMain.MOD_ID))).toList())
            .writingTo(infoTypeLabel)
            //.titled(filterSelectionBoxText.copy())
            .calling((i) -> {
                blockEntity.setInfoType(EDisplayInfo.getTypeById(i));
            })
            .setState(blockEntity.getInfoType().getId()));
        infoTypeInput.onChanged();

        doubleSidedButton = this.addRenderableWidget(new IconButton(guiLeft + 9, guiTop + 83, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.DOUBLE_SIDED.getAsCreateIcon()));
        doubleSidedButton.withCallback(() -> {
            // TODO
        });
        //buttonTooltips.put(soundButton, Pair.of(textNarrator, textNarratorDescription));
        doubleSidedIndicator = this.addRenderableWidget(new Indicator(guiLeft + 9, guiTop + 77, Components.immutableEmpty()));
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
            

        super.renderBg(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
}