package de.mrjulsen.crn.client.gui.widgets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.data.ISavableNavigatorData;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SavedRoutesViewer extends DLGuiComponent {   
    
    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;
    
    private List<? extends ISavableNavigatorData> data = List.of();

    public SavedRoutesViewer(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, width(), height()));
        contentPanel.anchor.set2(EAlign.values());
        contentPanel.inputConsumptionPolicy.set((type) -> false);
        
        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(10, 10, 10, 10));
        layout.wrap.set(false);
        layout.verticalGap.set(3);
        contentPanel.layout.set(layout);

        scrollbar = addComponent(new DLScrollBar(width() - 5, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
        scrollbar.scrollerSize.set(0);
        scrollbar.screenSize.set(contentPanel.height());
        scrollbar.scrollSteps.set(15);
        scrollbar.max.set(0);
        scrollbar.inputConsumptionPolicy.set((type) -> true);
        scrollbar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            contentPanel.setScrollOffsetY(e.value());
            return false;
        });
        
        addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollbar::invokeEvent);

        contentPanel.addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {            
            scrollbar.max.set(e.layoutResult().contentHeight());
            scrollbar.screenSize.set(contentPanel.height());
            return false;
        });
    }    

    public void refresh() {
        displaySavedRoutes(data);
    }

    public void displaySavedRoutes(List<? extends ISavableNavigatorData> data) {
        contentPanel.clearComponents();
        this.data = data;
        Collections.sort(data, Comparator
            .comparing(x -> ((ISavableNavigatorData)x).customGroup() == null ? null : ((ISavableNavigatorData)x).customGroup().getFirst(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingLong(x -> ((ISavableNavigatorData)x).dayOrderValue())
            .thenComparingLong(x -> ((ISavableNavigatorData)x).timeOrderValue()));
        
        ISavableNavigatorData lastData = null;
        for (int i = 0; i < data.size(); i++) {
            ISavableNavigatorData d = data.get(i);

            if (lastData != null && lastData.customGroup() != d.customGroup()) {
                contentPanel.addComponent(new GroupingHeader((d.customGroup() == null ? TextUtils.empty() : d.customGroup().getSecond()).withStyle(ChatFormatting.BOLD)));
            }
            if (lastData == null || lastData.dayOrderValue() != d.dayOrderValue()) {
                Component text;
                DLTime worldTime = DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem());
                long dayDiff = d.dayOrderValue() - (long)worldTime.toGameDays();

                if (d.timeOrderValue() < (long)worldTime.getTicks()) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.in_the_past");
                else if (dayDiff == 0) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.today");
                else if (dayDiff == 1) text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.tomorrow");
                else text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.in_days", dayDiff);

                contentPanel.addComponent(new GroupingHeader(text));
            }
            
            lastData = d;
            SavedRouteWidget widget = new SavedRouteWidget(this, 0, 0, d);
            contentPanel.addComponent(widget);
        }
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        if (scrollbar.canScroll() && scrollbar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
        if (scrollbar.canScroll() && scrollbar.value.get() < scrollbar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);
        }
    }

    private static final class GroupingHeader extends DLGuiComponent {

        private static final int HEIGHT = 24;

        private final Component text;

        public GroupingHeader(Component text) {
            super(0, 0, 100, HEIGHT);
            this.text = text == null ? TextUtils.empty() : text;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, height() / 2 - graphics.defaultFont().lineHeight / 2, text, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, true);
        }        
    }
    
}
