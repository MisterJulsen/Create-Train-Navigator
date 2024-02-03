package de.mrjulsen.crn.client.gui.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.widgets.ModEditBox;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.util.GuiUtils;
import de.mrjulsen.crn.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class TrainBlacklistScreen extends Screen implements IForegroundRendering {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final int ENTRIES_START_Y_OFFSET = 10;
    private static final int ENTRY_HEIGHT = 20;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 54;        
    private final int AREA_W = 220;
    private final int AREA_H = 156;


    private int guiLeft, guiTop;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;

    // Controls
    private IconButton backButton;
    private ModEditBox newEntryBox;
    private GuiAreaDefinition addButton;
	private ModStationSuggestions destinationSuggestions;
    private final Map<String, GuiAreaDefinition> blacklistEntryButton = new HashMap<>();

    // Tooltips
    private final Component tooltipAdd = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_blacklist.add.tooltip");
    private final Component tooltipRemove = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_blacklist.delete.tooltip");

    @SuppressWarnings("resource")
    public TrainBlacklistScreen(Level level, Screen lastScreen) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_blacklist.title"));
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

        newEntryBox = new ModEditBox(minecraft.font, guiLeft + AREA_X + 5 + 35, guiTop + AREA_Y - 28 + 10, 129, 12, new TextComponent(""));
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(25);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.setOnFocusChanged((box, focused) -> {
            if (!focused) {
                clearSuggestions();
            }
        });
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        addRenderableWidget(newEntryBox);

        addButton = new GuiAreaDefinition(guiLeft + AREA_X + 165 + 10, guiTop + AREA_Y - 28 + 6, 16, 16);

        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        blacklistEntryButton.clear();
        
        String[] names = GlobalSettingsManager.getInstance().getSettingsData().getTrainBlacklist().toArray(String[]::new);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            blacklistEntryButton.put(name, new GuiAreaDefinition(guiLeft + AREA_X + 165 + 10, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 2, 16, 16));
        }
    }

    private void addToBlacklist() {
        String value = newEntryBox.getValue();
        newEntryBox.setValue("");

        if (GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(value) || ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream().noneMatch(x -> x.equals(value))) {
            return;
        }

        GlobalSettingsManager.getInstance().getSettingsData().addTrainToBlacklist(value, () -> {
            initStationDeleteButtons();
        });
        
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
        newEntryBox.tick();
		scroll.tickChaser();

        if (destinationSuggestions != null) {
            destinationSuggestions.tick();

            if (!newEntryBox.canConsumeInput()) {
                clearSuggestions();
            }
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);        
        blit(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y - 28, 0, 110, 200, 28);
        blit(pPoseStack, guiLeft + AREA_X + 35, guiTop + AREA_Y - 28 + 5, 0, 92, 139, 18);        
        
        blit(pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 
        if (addButton.isInBounds(pMouseX, pMouseY)) { 
            fill(pPoseStack, addButton.getX(), addButton.getY(), addButton.getRight(), addButton.getBottom(), 0x1AFFFFFF);
        }
        
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        GuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        String[] blacklist = GlobalSettingsManager.getInstance().getSettingsData().getTrainBlacklist().toArray(String[]::new);
        for (int i = 0; i < blacklist.length; i++) {
            blit(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (i * ENTRY_HEIGHT), 0, 4, 200, ENTRY_HEIGHT);
        }
        blit(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT), 0, 23, 200, 3);
        blit(pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT) + 3, 0, 46, 200, 2);

        for (GuiAreaDefinition def : blacklistEntryButton.values()) { 
            blit(pPoseStack, def.getX(), def.getY(), 232, 0, 16, 16); // delete button

            if (def.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                fill(pPoseStack, def.getX(), def.getY(), def.getRight(), def.getBottom(), 0x1AFFFFFF);
            }
        }

        for (int i = 0; i < blacklist.length; i++) {
            String entry = blacklist[i];
            drawString(pPoseStack, shadowlessFont, entry, guiLeft + AREA_X + 40, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 6, 0xFFFFFF);
        }

        pPoseStack.popPose();
        GuiUtils.endStencil();        
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y - 38, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y - 38 + 10, 0x77000000, 0x00000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H, 0x00000000, 0x77000000);
        net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X + 10, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W - 10, guiTop + AREA_Y + 10, 0x77000000, 0x00000000);
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        // Scrollbar
        double maxHeight = getMaxScrollHeight();
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
        }
        
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = Utils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY));
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (destinationSuggestions != null) {
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 500);
			destinationSuggestions.render(matrixStack, mouseX, mouseY);
			matrixStack.popPose();
		}

		float scrollOffset = scroll.getValue(partialTicks);
        
        GuiUtils.renderTooltip(this, backButton, List.of(Constants.TOOLTIP_GO_BACK.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        GuiUtils.renderTooltip(this, addButton, List.of(tooltipAdd.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);

        if (mouseX > guiLeft + AREA_X && mouseX < guiLeft + AREA_X + AREA_W && mouseY > guiTop + AREA_Y && mouseY < guiTop + AREA_Y + AREA_H) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (GuiUtils.renderTooltip(this, entry.getValue(), List.of(tooltipRemove.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, (int)scrollOffset)) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
            return super.mouseClicked(pMouseX, pMouseY, pButton);

		float scrollOffset = scroll.getValue();
        
        if (addButton.isInBounds(pMouseX, pMouseY) && !newEntryBox.getValue().isBlank()) {
            addToBlacklist();
        } else if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H &&
                    blacklistEntryButton.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY + scrollOffset))) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY + scrollOffset)) {
                    GlobalSettingsManager.getInstance().getSettingsData().removeTrainFromBlacklist(entry.getKey(), () -> {
                        initStationDeleteButtons();
                    });
                    return super.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton);
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
            return true;

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private int getMaxScrollHeight() {
        return ENTRIES_START_Y_OFFSET + ENTRY_HEIGHT * GlobalSettingsManager.getInstance().getSettingsData().getTrainBlacklist().size();
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

		float chaseTarget = scroll.getChaseTarget();		
        float max = -AREA_H + getMaxScrollHeight();

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }

    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModStationSuggestions(minecraft, this, field, minecraft.font, getViableTrains(field), field.getHeight() + 2 + field.y);
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableTrains(EditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream()
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}
}
