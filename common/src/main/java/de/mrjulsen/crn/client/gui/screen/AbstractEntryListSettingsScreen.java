package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.AbstractEntryListOptionWidget;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.WidgetContainerCollection;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public abstract class AbstractEntryListSettingsScreen<D, W extends AbstractEntryListOptionWidget> extends DLScreen {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/settings.png");
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

    private boolean initialized = false;

    // Data
    private final Level level;
    private final Font shadowlessFont;
    private final Screen lastScreen;
    private boolean initEntries = false;
    private boolean renderingEntries = false;

    // Widgets
    private final WidgetContainerCollection entriesCollection = new WidgetContainerCollection();
    private final WidgetsCollection newEntryCollection = new WidgetsCollection();
    private DLEditBox newEntryInputBox;
    private DLCreateIconButton backButton;
    private DLCreateIconButton addButton;
    private DLEditBox searchBox;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    // Tooltips
    private final MutableComponent tooltipAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.add.tooltip");
    private final MutableComponent textEnterName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.enter_name");

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
        initialized = false;
        super.init();
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;


        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK)
        .withCallback((x, y) -> {
            onClose();
        }));
        addTooltip(DLTooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));

        addButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_ADD));
        addButton.withCallback((x, y) -> {
            addButton.visible = false;
            newEntryCollection.setVisible(true);
            newEntryInputBox.setValue("");
        });
        addTooltip(DLTooltip.of(tooltipAdd).assignedTo(addButton));

        searchBox = addEditBox(guiLeft + AREA_X + 1, guiTop + 16 + 1, AREA_W - 2, 16, "", Constants.TEXT_SEARCH, true, (b) -> {
            refreshEntries();
        }, NO_EDIT_BOX_FOCUS_CHANGE_ACTION, null);

        // Add new Entry
        newEntryCollection.add(addRenderableWidget(new DLCreateIconButton(guiLeft + 145, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM))
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

        newEntryCollection.add(addRenderableWidget(new DLCreateIconButton(guiLeft + 145 + DEFAULT_ICON_BUTTON_WIDTH, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_DISABLE))
        .withCallback((x, y) -> {
            cancelNewEntryCreation(null);
        }));

        newEntryCollection.add(newEntryInputBox = addEditBox(guiLeft + 44, guiTop + 223, 100, 16, "", textEnterName, true, (b) -> {}, (box, focus) -> {}, null));

        initialized = true;
        newEntryCollection.setVisible(false);
        refreshEntries();
    }

    protected abstract void onCreateNewEntry(String value, Runnable refreshAction);

    private boolean cancelFocus = false;
    private void cancelNewEntryCreation(DLEditBox excluded) {
        if (cancelFocus) {
            return;
        }
        cancelFocus = true;
        addButton.visible = true;
        newEntryCollection.setVisible(false);     
        //renderables.stream().filter(x -> x instanceof DLEditBox && excluded != x).forEach(x -> ((DLEditBox)x).setFocus(false));
        cancelFocus = false;
    }

    protected void refreshEntries() {
        if (!initialized) {
            return;
        }
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
        newEntryCollection.performForEachOfType(DLEditBox.class, x -> x.tick());
    }

    public int getScrollOffset(float pPartialTicks) {
        return (int)scroll.getValue(pPartialTicks);
    }

    private float getScrollMax() {
        float max = -AREA_H + (ENTRIES_START_Y_OFFSET * 2);
        for (WidgetContainer w : entriesCollection.components) {
            max += 4 + w.getHeight();
        }
        return max;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {   
        pPartialTick = Minecraft.getInstance().getFrameTime();      
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 19, guiTop + 4, title, 0x4F4F4F, EAlignment.LEFT, false);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, TextUtils.text(timeString), 0x4F4F4F, EAlignment.LEFT, false);

        GuiUtils.enableScissor(graphics, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        graphics.poseStack().translate(0, scrollOffset, 0);

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
                    widget.render(graphics.graphics(), pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
                }
                currentY += widget.getHeight() + ENTRY_SPACING;
            }
            
        }

        //super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
        renderingEntries = false;

        GuiUtils.disableScissor(graphics);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y, 0, AREA_W, 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, 0, AREA_W, 10, 0x00000000, 0x77000000);
        
        // Scrollbar
        int componentHeight = entriesCollection.components.stream().mapToInt(x -> x.getHeight() + ENTRY_SPACING).sum();
        double maxHeight = ENTRIES_START_Y_OFFSET * 2 + componentHeight;
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            GuiUtils.fill(graphics, guiLeft + AREA_X + AREA_W - 3, startY, 3, scrollerHeight, 0x7FFFFFFF);
        }

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTicks) {
        entriesCollection.performForEachOfType(AbstractEntryListOptionWidget.class, x -> {      
            if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
                x.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTicks);
            }      
            x.renderSuggestions(graphics, pMouseX, pMouseY, pPartialTicks);
        });
        
        super.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTicks);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		float scrollOffset = scroll.getValue();
        for (WidgetContainer w : entriesCollection.components) {
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

        for (WidgetContainer w : entriesCollection.components) {
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
			chaseTarget = MathUtils.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        for (WidgetContainer w : entriesCollection.components) {
            if (w.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (WidgetContainer w : entriesCollection.components) {
            if (w.charTyped(pCodePoint, pModifiers)) {
                return true;
            }
        }
        return super.charTyped(pCodePoint, pModifiers);
    }
}
