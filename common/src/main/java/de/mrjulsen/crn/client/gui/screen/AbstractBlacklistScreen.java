package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public abstract class AbstractBlacklistScreen extends DLScreen {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/settings.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/settings_widgets.png");
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

    private boolean initialized;
    
    // Controls
    private DLCreateIconButton backButton;
    private DLEditBox newEntryBox;
    private DLEditBox searchBox;
    private GuiAreaDefinition addButton;
	private ModStationSuggestions suggestions;
    private final Map<String, GuiAreaDefinition> blacklistEntryButton = new HashMap<>();
    private final Map<String, GuiAreaDefinition> entryAreas = new HashMap<>();

    // Tooltips
    private final MutableComponent tooltipAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".blacklist.add.tooltip");
    private final MutableComponent tooltipRemove = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".blacklist.delete.tooltip");

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
        initialized = false;
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;      

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });
        addTooltip(DLTooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));

        newEntryBox = addEditBox(guiLeft + AREA_X + 5 + 35, guiTop + AREA_Y - 28 + 10, 129, 12, "", TextUtils.empty(), false, (s) -> {
            updateEditorSubwidgets(newEntryBox);
        }, (box, focus) -> {
            if (!focus) {
                clearSuggestions();
            }
        }, null);
		newEntryBox.setMaxLength(25);
		newEntryBox.setTextColor(0xFFFFFF);

        addButton = new GuiAreaDefinition(guiLeft + AREA_X + 165 + 10, guiTop + AREA_Y - 28 + 6, 16, 16);
        addTooltip(DLTooltip.of(tooltipAdd).assignedTo(addButton));

        searchBox = addEditBox(guiLeft + AREA_X + 1, guiTop + 16 + 1, AREA_W - 2, 16, "", Constants.TEXT_SEARCH, true, (s) -> {
            initStationDeleteButtons();
        }, null, null);

        initialized = true;
        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        if (!initialized) {
            return;
        }

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

    protected void updateEditorSubwidgets(DLEditBox field) {
        if (!initialized) {
            return;
        }

        clearSuggestions();

		suggestions = new ModStationSuggestions(minecraft, this, field, minecraft.font, getViableStations(field), field.getHeight() + 2 + field.y);
        suggestions.setAllowSuggestions(true);
        suggestions.updateCommandInfo();
	}


    private List<String> getViableStations(DLEditBox field) {
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
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        pPartialTick = Minecraft.getInstance().getFrameTime();
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
               
        GuiUtils.drawTexture(WIDGETS, graphics, guiLeft + AREA_X + 10, guiTop + AREA_Y - 28, 0, 110, 200, 28);
        GuiUtils.drawTexture(WIDGETS, graphics, guiLeft + AREA_X + 35, guiTop + AREA_Y - 28 + 5, 0, 92, 139, 18);        
        GuiUtils.drawTexture(WIDGETS, graphics, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 
        if (addButton.isInBounds(pMouseX, pMouseY)) { 
            GuiUtils.fill(graphics, addButton.getX(), addButton.getY(), addButton.getWidth(), addButton.getHeight(), 0x1AFFFFFF);
        }
        
        GuiUtils.enableScissor(graphics, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        graphics.poseStack().translate(0, scrollOffset, 0);

        String[] blacklist = getBlacklistedNames(searchBox.getValue());
        for (int i = 0; i < blacklist.length; i++) {
            GuiUtils.drawTexture(WIDGETS, graphics, guiLeft + AREA_X + 10, guiTop + AREA_Y + (i * ENTRY_HEIGHT), 0, 4, 200, ENTRY_HEIGHT);
        }
        GuiUtils.drawTexture(WIDGETS, graphics, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT), 0, 23, 200, 3);
        GuiUtils.drawTexture(WIDGETS, graphics, guiLeft + AREA_X + 10, guiTop + AREA_Y + (blacklist.length * ENTRY_HEIGHT) + 3, 0, 46, 200, 2);

        for (GuiAreaDefinition def : blacklistEntryButton.values()) { 
            GuiUtils.drawTexture(WIDGETS, graphics, def.getX(), def.getY(), 232, 0, 16, 16); // delete button

            if (def.isInBounds(pMouseX, pMouseY - scrollOffset)) {
                GuiUtils.fill(graphics, def.getX(), def.getY(), def.getWidth(), def.getHeight(), 0x1AFFFFFF);
            }
        }

        for (int i = 0; i < blacklist.length; i++) {
            MutableComponent name = TextUtils.text(blacklist[i]);
            int maxTextWidth = 129;  
            if (shadowlessFont.width(name) > maxTextWidth) {
                name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
            }
            GuiUtils.drawString(graphics, shadowlessFont, guiLeft + AREA_X + 40, guiTop + AREA_Y + (i * ENTRY_HEIGHT) + 6, name, 0xFFFFFF, EAlignment.LEFT, false);
        }

        GuiUtils.disableScissor(graphics);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y - 38, 0, AREA_W, 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, 0, AREA_W, 10, 0x00000000, 0x77000000);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X + 10, guiTop + AREA_Y, 0, AREA_W - 20, 10, 0x77000000, 0x00000000);

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);

        // Scrollbar
        double maxHeight = getMaxScrollHeight();
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            GuiUtils.fill(graphics, guiLeft + AREA_X + AREA_W - 3, startY, 3, scrollerHeight, 0x7FFFFFFF);
        }
        
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 19, guiTop + 4, title, 0xFF4F4F4F, EAlignment.LEFT, false);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, TextUtils.text(timeString), 0xFF4F4F4F, EAlignment.LEFT, false);
    }

    
    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        if (suggestions != null) {
			graphics.poseStack().pushPose();
			graphics.poseStack().translate(0, 0, 500);
			suggestions.render(graphics.poseStack(), mouseX, mouseY);
			graphics.poseStack().popPose();
		}

		float scrollOffset = scroll.getValue(partialTick);
        
        if (mouseX > guiLeft + AREA_X && mouseX < guiLeft + AREA_X + AREA_W && mouseY > guiTop + AREA_Y && mouseY < guiTop + AREA_Y + AREA_H) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (GuiUtils.renderTooltipWithOffset(this, entry.getValue(), List.of(tooltipRemove), width, graphics, mouseX, mouseY, 0, (int)scrollOffset)) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : entryAreas.entrySet()) {
                if (shadowlessFont.width(entry.getKey()) > 129 && GuiUtils.renderTooltipAt(this, entry.getValue(), List.of(TextUtils.text(entry.getKey())), width, graphics, entry.getValue().getLeft() + 1, (int)(entry.getValue().getTop() - scrollOffset), mouseX, mouseY, 0, (int)scrollOffset)) {
                    break;
                }
            }
        }
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        if (suggestions != null && suggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton)) {            
            GuiUtils.playButtonSound();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

		float scrollOffset = scroll.getValue();
        
        if (addButton.isInBounds(pMouseX, pMouseY) && !newEntryBox.getValue().isBlank()) {
            addToBlacklistInternal();            
            GuiUtils.playButtonSound();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H && blacklistEntryButton.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY + scrollOffset))) {
            for (Entry<String, GuiAreaDefinition> entry : blacklistEntryButton.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY + scrollOffset)) {
                    removeFromBlacklist(entry.getKey(), this::initStationDeleteButtons);
                    GuiUtils.playButtonSound();
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
