package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.widgets.AbstractEntryListOptionWidget;
import de.mrjulsen.crn.client.gui.widgets.HintTextBox;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.Tooltip;
import de.mrjulsen.mcdragonlib.client.gui.WidgetsCollection;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public abstract class AbstractEntryListSettingsScreen<D, W extends AbstractEntryListOptionWidget> extends CommonScreen {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 10;
    private final int ENTRY_SPACING = 4;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 16 + 18; // + searchbar height
    private final int AREA_W = 220;
    private final int AREA_H = 194 - 18; // + searchbar height

    private int guiLeft, guiTop;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private boolean initEntries = false;
    private boolean renderingEntries = false;

    // Widgets
    private final WidgetsCollection entriesCollection = new WidgetsCollection();
    private final WidgetsCollection newEntryCollection = new WidgetsCollection();
    private HintTextBox newEntryInputBox;
    private IconButton backButton;
    private IconButton addButton;
    private HintTextBox searchBox;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Tooltips
    private final MutableComponent tooltipAdd = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.add.tooltip");
    private final String textEnterName = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.enter_name").getString();

    public static record WidgetCreationData<D, W extends AbstractEntryListSettingsScreen<D, ?>>(W parent, int x, int y, List<?> previousEntries) {}

    @SuppressWarnings("resource")
    public AbstractEntryListSettingsScreen(Level level, Screen lastScreen, Component title) {
        super(title);
        this.level = level;
        this.lastScreen = lastScreen;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
    }

    protected abstract D[] getData(String searchText);
    protected abstract W createWidget(WidgetCreationData<D, AbstractEntryListSettingsScreen<D, W>> widgetData, D data);

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        backButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK)
        .withCallback((x, y) -> {
            onClose();
        }));
        addTooltip(Tooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));

        addButton = this.addRenderableWidget(new IconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_ADD));
        addButton.withCallback((x, y) -> {
            addButton.visible = false;
            newEntryCollection.setVisible(true);
            newEntryInputBox.setValue("");
        });
        addTooltip(Tooltip.of(tooltipAdd).assignedTo(addButton));

        searchBox = addRenderableWidget(new HintTextBox(font, guiLeft + AREA_X + 1, guiTop + 16 + 1, AREA_W - 2, 16));
        searchBox.setHint(Constants.TEXT_SEARCH.getString());
        searchBox.setResponder(text -> {
            refreshEntries();
        });

        // Add new Entry
        newEntryCollection.add(addRenderableWidget(new IconButton(guiLeft + 145, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM))
        .withCallback((x, y) -> {
            if (newEntryInputBox.getValue().isBlank()) {
                return;
            }

            onCreateNewEntry(newEntryInputBox.getValue(), () -> {
                refreshEntries();
                scroll.chase(getScrollMax(), 0.7f, Chaser.EXP);
            });

            cancelNewEntryCreation(null);
        }));

        newEntryCollection.add(addRenderableWidget(new IconButton(guiLeft + 145 + DEFAULT_ICON_BUTTON_WIDTH, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_DISABLE))
        .withCallback((x, y) -> {
            cancelNewEntryCreation(null);
        }));

        newEntryCollection.add(newEntryInputBox = addRenderableWidget(new HintTextBox(font, guiLeft + 44, guiTop + 223, 100, 16)));
        newEntryInputBox.setHint(textEnterName);
        newEntryInputBox.setOnFocusChanged((box, focus) -> {
            if (!focus) {
                cancelNewEntryCreation(null);
            }
        });

        newEntryCollection.setVisible(false);
        refreshEntries();
    }

    protected abstract void onCreateNewEntry(String value, Runnable refreshAction);

    private boolean cancelFocus = false;
    private void cancelNewEntryCreation(EditBox excluded) {
        if (cancelFocus) {
            return;
        }
        cancelFocus = true;
        addButton.visible = true;
        newEntryCollection.setVisible(false);     
        renderables.stream().filter(x -> x instanceof EditBox && excluded != x).forEach(x -> ((EditBox)x).setFocus(false));
        entriesCollection.performForEachOfType(AbstractEntryListOptionWidget.class, x -> x.unfocusAll());
        cancelFocus = false;
    }

    protected void refreshEntries() {
        initEntries = true;
        
        while (renderingEntries) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        

        int startY = guiTop + AREA_X + ENTRIES_START_Y_OFFSET; 
        List<?> previousComponents = new ArrayList<>(entriesCollection.components);
        entriesCollection.components.forEach(x -> removeWidget(x));
        entriesCollection.components.clear();

        // Entries
        D[] data = getData(searchBox.getValue());
        for (int i = 0; i < data.length; i++) {
            W w = createWidget(new WidgetCreationData<D, AbstractEntryListSettingsScreen<D, W>>(this, guiLeft + AREA_X + 10, startY, previousComponents), data[i]);
            entriesCollection.components.add(w);
            w.calcHeight();
        }
        initEntries = false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void tick() {
        searchBox.tick();
		scroll.tickChaser();

        float scrollMax = getScrollMax();
        if (scroll.getValue() > 0 && scroll.getValue() > scrollMax) {
            scroll.chase(Math.max(0, getScrollMax()), 0.7f, Chaser.EXP);
        }

        entriesCollection.performForEachOfType(AbstractEntryListOptionWidget.class, x -> x.tick());
        newEntryCollection.performForEachOfType(EditBox.class, x -> x.tick());
    }

    public int getScrollOffset(float pPartialTicks) {
        return (int)scroll.getValue(pPartialTicks);
    }

    private float getScrollMax() {
        float max = -AREA_H + (ENTRIES_START_Y_OFFSET * 2);
        for (AbstractWidget w : entriesCollection.components) {
            max += 4 + w.getHeight();
        }
        return max;
    }
    
    public void unfocusAllWidgets() {
        renderables.stream().filter(x -> x instanceof EditBox).forEach(x -> ((EditBox)x).setFocus(false));
    }

    public void unfocusAllEntries() {
        entriesCollection.performForEachOfType(AbstractEntryListOptionWidget.class, x -> x.unfocusAll());
    }

    @Override
    public void renderBg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {         
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        GuiUtils.blit(GUI, pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % DragonLibConstants.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);
                
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        pPoseStack.pushPose();
        pPoseStack.translate(0, scrollOffset, 0);

        renderingEntries = true;
        while (initEntries) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // render entries
        int currentY = guiTop + AREA_Y + ENTRIES_START_Y_OFFSET;
        for (int i = 0; i < entriesCollection.components.size(); i++) {
            if (entriesCollection.components.get(i) instanceof AbstractEntryListOptionWidget widget) {
                widget.setYPos(currentY);
                widget.calcHeight();
                if (currentY < guiTop + AREA_Y + AREA_H - scrollOffset && currentY + widget.getHeight() > guiTop + AREA_Y - scrollOffset) {
                    widget.render(pPoseStack, pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
                }
                currentY += widget.getHeight() + ENTRY_SPACING;
            }
        }
        renderingEntries = false;

        pPoseStack.popPose();
        ModGuiUtils.endStencil();        
        net.minecraftforge.client.gui.ScreenUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10, 0x77000000, 0x00000000);
        net.minecraftforge.client.gui.ScreenUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H, 0x00000000, 0x77000000);
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());
        
        // Scrollbar
        int componentHeight = entriesCollection.components.stream().mapToInt(x -> x.getHeight() + ENTRY_SPACING).sum();
        double maxHeight = ENTRIES_START_Y_OFFSET * 2 + componentHeight;
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
        }

        super.renderBg(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderFg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTicks) {
        entriesCollection.performForEachOfType(AbstractEntryListOptionWidget.class, x -> {
            if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
                x.renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTicks);
            }
            x.renderSuggestions(pPoseStack, pMouseX, pMouseY, pPartialTicks);
        });
        super.renderFg(pPoseStack, pMouseX, pMouseY, pPartialTicks);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		float scrollOffset = scroll.getValue();
        for (AbstractWidget w : entriesCollection.components) {
            if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
                if (w.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton)) {
                    break;
                }                            
            }

            if (w instanceof AbstractEntryListOptionWidget entry && entry.mouseClickedLoop(pMouseX, pMouseY + scrollOffset, pButton)) {
                break;
            }                
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
		float scrollOffset = scroll.getValue();

        for (AbstractWidget w : entriesCollection.components) {
            if (w instanceof AbstractEntryListOptionWidget entry && entry.mouseScrolledLoop(pMouseX, pMouseY + scrollOffset, pDelta)) {
                return true;
            }

            if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
                if (w.mouseScrolled(pMouseX, pMouseY + scrollOffset, pDelta)) {
                    return true;
                }                            
            }             
        }

		float chaseTarget = scroll.getChaseTarget();		
        float max = getScrollMax();

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        for (AbstractWidget w : entriesCollection.components) {
            if (w.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (AbstractWidget w : entriesCollection.components) {
            if (w.charTyped(pCodePoint, pModifiers)) {
                return true;
            }
        }
        return super.charTyped(pCodePoint, pModifiers);
    }
}
