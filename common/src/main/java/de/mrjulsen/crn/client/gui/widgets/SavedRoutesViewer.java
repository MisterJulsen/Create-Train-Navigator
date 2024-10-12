package de.mrjulsen.crn.client.gui.widgets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.ISaveableNavigatorData;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SavedRoutesViewer extends ScrollableWidgetContainer {

    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;

    private List<? extends ISaveableNavigatorData> data = List.of();

    public SavedRoutesViewer(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.updateMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public Screen getParent() {
        return parent;
    }

    public void refresh() {
        displayRoutes(data);
    }

    public void displayRoutes(List<? extends ISaveableNavigatorData> data) {
        this.data = data;
        Collections.sort(data, Comparator
            .comparing(x -> ((ISaveableNavigatorData)x).customGroup() == null ? null : ((ISaveableNavigatorData)x).customGroup().getFirst(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingLong(x -> ((ISaveableNavigatorData)x).dayOrderValue())
            .thenComparingLong(x -> ((ISaveableNavigatorData)x).timeOrderValue()));
        
        clearWidgets();
        contentHeight = 5;
        ISaveableNavigatorData lastData = null;
        for (int i = 0; i < data.size(); i++) {
            ISaveableNavigatorData d = data.get(i);

            if (lastData != null && lastData.customGroup() != d.customGroup()) {
                contentHeight += addRenderableOnly(new GroupingHeader(x(), y() + contentHeight, width(), (d.customGroup() == null ? TextUtils.empty() : d.customGroup().getSecond()).withStyle(ChatFormatting.BOLD))).height();
            }
            if (lastData == null || lastData.dayOrderValue() != d.dayOrderValue()) {
                Component text;
                long worldTime = DragonLib.getCurrentWorldTime();
                long dayDiff = d.dayOrderValue() - (worldTime + DragonLib.DAYTIME_SHIFT) / DragonLib.TICKS_PER_DAY;
                if (d.timeOrderValue() < worldTime) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.in_the_past");
                else if (dayDiff == 0) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.today");
                else if (dayDiff == 1) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.tomorrow");
                else text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.in_days", dayDiff);
                contentHeight += addRenderableOnly(new GroupingHeader(x(), y() + contentHeight, width(), text)).height();
            }
            
            lastData = d;
            SavedRouteWidget widget = new SavedRouteWidget(this, x(), y() + contentHeight, d);
            addRenderableWidget(widget);
            widget.set_x(x() + width() / 2 - widget.width() / 2);
            contentHeight += (widget.height() + 3);

        }
        contentHeight += 10;
        scrollBar.updateMaxScroll(contentHeight);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

        if (children().isEmpty()) {
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".empty_list"), 0xFFDBDBDB, EAlignment.CENTER, false);
        }
        
        GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }

    private static final class GroupingHeader extends DLRenderable {

        private static final int HEIGHT = 24;

        private final Component text;

        public GroupingHeader(int x, int y, int width, Component text) {
            super(x, y, width, HEIGHT);
            this.text = text == null ? TextUtils.empty() : text;
        }

        @SuppressWarnings("resource")
        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, x() + 10, y() + height() / 2 - Minecraft.getInstance().font.lineHeight / 2, text, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, true);
        }        
    }
    
}
