package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.widgets.HintTextBox;
import de.mrjulsen.crn.client.gui.widgets.ModEditBox;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.Tooltip;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public abstract class AbstractBlacklistScreen extends CommonScreen {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final int ENTRIES_START_Y_OFFSET = 10;
    private static final int ENTRY_HEIGHT = 20;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 54 + 18;        
    private final int AREA_W = 220;
    private final int AREA_H = 156 - 18;


    private int guiLeft, guiTop;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;

    // Controls
    private IconButton backButton;
    private ModEditBox newEntryBox;
    private HintTextBox searchBox;
    private GuiAreaDefinition addButton;
	private ModStationSuggestions suggestions;
    private final Map<String, GuiAreaDefinition> blacklistEntryButton = new HashMap<>();
    private final Map<String, GuiAreaDefinition> entryAreas = new HashMap<>();

    // Tooltips
    private final MutableComponent tooltipAdd = Utils.translate("gui." + ModMain.MOD_ID + ".blacklist.add.tooltip");
    private final MutableComponent tooltipRemove = Utils.translate("gui." + ModMain.MOD_ID + ".blacklist.delete.tooltip");

    @SuppressWarnings("resource")
    public AbstractBlacklistScreen(Level level, Screen lastScreen, Component title) {
        super(title);
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
    }
    
    protected abstract Collection<String> getSuggestions();
    protected abstract boolean checkIsBlacklisted(String entry);
    protected abstract String[] getBlacklistedNames(String searchText);
    protected abstract void addToBlacklist(String name, Runnable andThen);
    protected abstract void removeFromBlacklist(String name, Runnable andThen);

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
        addTooltip(Tooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));

        newEntryBox = new ModEditBox(minecraft.font, guiLeft + AREA_X + 5 + 35, guiTop + AREA_Y - 28 + 10, 129, 12, Utils.emptyText());
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
        addTooltip(Tooltip.of(tooltipAdd).assignedTo(addButton));

        searchBox = addRenderableWidget(new HintTextBox(font, guiLeft + AREA_X + 1, guiTop + 16 + 1, AREA_W - 2, 16));
        searchBox.setHint(Constants.TEXT_SEARCH);
        searchBox.setResponder(text -> {
            initStationDeleteButtons();
        });

        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        blacklistEntryButton.clear();
        entryAreas.clear();
        
        String[] names = getBlacklistedNames(searchBox.getValue());
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            blacklistEntryButton.put(name, new GuiAreaDefinition(guiLeft + AREA_X + 165 + 10, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 2, 16, 16));
            entryAreas.put(name, new GuiAreaDefinition(guiLeft + AREA_X + 35, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 2, 129, 16));
        }
    }

    private void addToBlacklistInternal() {
        String value = newEntryBox.getValue();
        newEntryBox.setValue("");

        if (value.isBlank()) {
            return;
        }

        addToBlacklist(value, this::initStationDeleteButtons);        
    }

    private int getMaxScrollHeight() {
        return ENTRIES_START_Y_OFFSET + ENTRY_HEIGHT * getBlacklistedNames(searchBox.getValue()).length;
    }

    private void clearSuggestions() {
        if (suggestions != null) {
            suggestions.getEditBox().setSuggestion("");
        }
        suggestions = null;
    }

    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		suggestions = new ModStationSuggestions(minecraft, this, field, minecraft.font, getViableStations(field), field.getHeight() + 2 + field.y);
        suggestions.setAllowSuggestions(true);
        suggestions.updateCommandInfo();
	}


    private List<String> getViableStations(EditBox field) {
        return getSuggestions().stream()
            .distinct()
            .filter(x -> !checkIsBlacklisted(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
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
        
        float scrollMax = getMaxScrollHeight();
        if (scroll.getValue() > 0 && scroll.getValue() > scrollMax) {
            scroll.chase(Math.max(0, scrollMax), 0.7f, Chaser.EXP);
        }

        if (suggestions != null) {
            suggestions.tick();

            if (!newEntryBox.canConsumeInput()) {
                clearSuggestions();
            }
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        GuiUtils.blit(GUI, pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
               
        GuiUtils.blit(WIDGETS, pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y - 28, 0, 110, 200, 28);
        GuiUtils.blit(WIDGETS, pPoseStack, guiLeft + AREA_X + 35, guiTop + AREA_Y - 28 + 5, 0, 92, 139, 18);        
        GuiUtils.blit(WIDGETS, pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 
        if (addButton.isInBounds(pMouseX, pMouseY)) { 
            fill(pPoseStack, addButton.getX(), addButton.getY(), addButton.getRight(), addButton.getBottom(), 0x1AFFFFFF);
        }
        
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        String[] blacklist = getBlacklistedNames(searchBox.getValue());
        for (int i = 0; i < blacklist.length; i++) {
            GuiUtils.blit(WIDGETS, pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (i * ENTRY_HEIGHT), 0, 4, 200, ENTRY_HEIGHT);
        }
        GuiUtils.blit(WIDGETS, pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT), 0, 23, 200, 3);
        GuiUtils.blit(WIDGETS, pPoseStack, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT) + 3, 0, 46, 200, 2);

        for (GuiAreaDefinition def : blacklistEntryButton.values()) { 
            GuiUtils.blit(WIDGETS, pPoseStack, def.getX(), def.getY(), 232, 0, 16, 16); // delete button

            if (def.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                fill(pPoseStack, def.getX(), def.getY(), def.getRight(), def.getBottom(), 0x1AFFFFFF);
            }
        }

        for (int i = 0; i < blacklist.length; i++) {
            MutableComponent name = Utils.text(blacklist[i]);
            int maxTextWidth = 129;  
            if (shadowlessFont.width(name) > maxTextWidth) {
                name = Utils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
            }
            drawString(pPoseStack, shadowlessFont, name, guiLeft + AREA_X + 40, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 6, 0xFFFFFF);
        }

        pPoseStack.popPose();
        ModGuiUtils.endStencil();        
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
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);
    }

    @Override
    public void renderFg(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (suggestions != null) {
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 500);
			suggestions.render(matrixStack, mouseX, mouseY);
			matrixStack.popPose();
		}

		float scrollOffset = scroll.getValue(partialTicks);
        
        if (mouseX > guiLeft + AREA_X && mouseX < guiLeft + AREA_X + AREA_W && mouseY > guiTop + AREA_Y && mouseY < guiTop + AREA_Y + AREA_H) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (GuiUtils.renderTooltipWithScrollOffset(this, entry.getValue(), List.of(tooltipRemove), width, matrixStack, mouseX, mouseY, 0, (int)scrollOffset)) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : entryAreas.entrySet()) {
                if (shadowlessFont.width(entry.getKey()) > 129 && ModGuiUtils.renderTooltipAtFixedPos(this, entry.getValue(), List.of(Utils.text(entry.getKey())), width, matrixStack, mouseX, mouseY, 0, (int)scrollOffset, entry.getValue().getLeft() + 1, (int)(entry.getValue().getTop() - scrollOffset))) {
                    break;
                }
            }
        }
        super.renderFg(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        if (suggestions != null && suggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton)) {            
            ModGuiUtils.playButtonSound();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

		float scrollOffset = scroll.getValue();
        
        if (addButton.isInBounds(pMouseX, pMouseY) && !newEntryBox.getValue().isBlank()) {
            addToBlacklistInternal();            
            ModGuiUtils.playButtonSound();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H && blacklistEntryButton.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY + scrollOffset))) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY + scrollOffset)) {
                    removeFromBlacklist(entry.getKey(), this::initStationDeleteButtons);
                    ModGuiUtils.playButtonSound();
                    return super.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton);
                }
            }
        }
        
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (suggestions != null && suggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
            return true;

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (suggestions != null && suggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
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
}
