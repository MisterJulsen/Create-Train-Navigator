package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.data.settings.UserSettings;
import de.mrjulsen.crn.util.EDepartureBoardTrainFilter;
import de.mrjulsen.crn.api.core.BoardEntry;
import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.network.packets.GetStationBoardPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
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
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;
import net.minecraft.client.Minecraft;

public class StationDeparturesViewer extends DLGuiComponent {

    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;

    public final BooleanProperty expanded = new BooleanProperty(false);
    public final BooleanProperty showTrainDetails = new BooleanProperty(true);
    public final BooleanProperty showEntireJourney = new BooleanProperty(false);
    public final BooleanProperty canExpandCollapse = new BooleanProperty(true);

    public StationDeparturesViewer(int x, int y, int w, int h) {
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

    public void displayDepartures(UUID stationTagId, UserSettings settings) {
        contentPanel.clearComponents();
        if (stationTagId == null) {
            return;
        }

        ModNetworkManager.GET_STATION_BOARD.send(NetworkDirection.toServer(),
            new GetStationBoardPacketData.Request(stationTagId, null, Minecraft.getInstance().player.getUUID(), GetStationBoardPacketData.UNLIMITED),
            (response) -> {
                for (Row row : rowsOf(response.getEntries(), settings.searchTrainFilter.getValue())) {
                    contentPanel.addComponent(new StationDeparturesWidget(this, row.entry(), row.direction()));
                }
            }, () -> {});
    }

    private record Row(BoardEntry entry, CallDirection direction) {

        long time() {
            return entry.scheduledTime(direction);
        }
    }

    private static List<Row> rowsOf(List<BoardEntry> entries, EDepartureBoardTrainFilter filter) {
        List<Row> rows = new ArrayList<>(entries.size());
        for (BoardEntry entry : entries) {
            if (!entry.originating() && filter != EDepartureBoardTrainFilter.DEPARTURE_ONLY) {
                rows.add(new Row(entry, CallDirection.ARRIVAL));
            }
            if (!entry.terminus() && filter != EDepartureBoardTrainFilter.ARRIVAL_ONLY) {
                rows.add(new Row(entry, CallDirection.DEPARTURE));
            }
        }
        rows.sort(Comparator.comparingLong(Row::time));
        return rows;
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
}
