package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.SettingFlyout;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationTagsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTimeSelectionComponent;
import de.mrjulsen.crn.core.navigator.Waypoint;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLNumberPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public class WaypointsContent extends FlyoutContent {

    private static final int WIDTH = 200;
    private static final int BORDER = 4;
    private static final int GAP = 2;
    private static final int MAX_WAYPOINTS = 5;

    private final MutableComponent title;
    private final Supplier<UserSetting<List<Waypoint>>> getUserSetting;

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<WaypointEntry> pendingRemoval = new ArrayList<>();
    private boolean structureDirty = false;

    public WaypointsContent(MutableComponent title, Supplier<UserSetting<List<Waypoint>>> getUserSetting) {
        super(WIDTH, WaypointEntry.HEIGHT + BORDER * 2);
        this.title = title;
        this.getUserSetting = getUserSetting;
    }

    @Override
    protected void onMount() {
        FlowLayout layout = new FlowLayout();
        layout.wrap.set(false);
        layout.flowDirection.set(FlowLayout.Direction.VERTICAL);
        layout.fillCrossAxis.set(true);
        layout.padding.set(new Padding(0, BORDER, BORDER, BORDER));
        layout.verticalGap.set(GAP);
        this.layout.set(layout);

        if (host instanceof SettingFlyout f) {
            setWidth(Math.max(WIDTH, f.getRequiredWidth(this)));
        }

        addEventListener(DLGuiStandardEvents.TickEvent.class, (s, e) -> {
            if (structureDirty) {
                structureDirty = false;
                reconcile();
            }
            return false;
        });
    }

    @Override
    public void onShow() {
        closeAllAutocompletes();
        clearComponents();

        waypoints.clear();
        waypoints.addAll(getUserSetting.get().getValue());
        if (waypoints.size() > MAX_WAYPOINTS) {
            waypoints.subList(MAX_WAYPOINTS, waypoints.size()).clear();
        }

        for (Waypoint waypoint : waypoints) {
            addComponent(new WaypointEntry(waypoint));
        }
        if (waypoints.size() < MAX_WAYPOINTS) {
            addComponent(new WaypointEntry(null));
        }
        updateRowStates();
        applySize();
    }

    @Override
    public void onHide() {
        closeAllAutocompletes();
    }

    @Override
    public void resetToDefaults() {
        getUserSetting.get().setToDefault();
        onShow();
    }

    @Override
    public MutableComponent getTitle() {
        return title;
    }

    private List<WaypointEntry> entries() {
        List<WaypointEntry> list = new ArrayList<>();
        for (DLGuiComponent component : getComponents()) {
            if (component instanceof WaypointEntry entry) {
                list.add(entry);
            }
        }
        return list;
    }

    private void closeAllAutocompletes() {
        for (WaypointEntry entry : entries()) {
            entry.closeAutocomplete();
        }
    }

    private void persistModel() {
        Set<String> seen = new HashSet<>();
        List<Waypoint> model = new ArrayList<>();
        for (WaypointEntry entry : entries()) {
            String station = entry.getStationText().trim();
            if (station.isBlank() || !seen.add(station.toLowerCase(Locale.ROOT))) {
                continue;
            }
            model.add(new Waypoint(station, entry.getMinStay()));
        }
        waypoints.clear();
        waypoints.addAll(model);
        getUserSetting.get().setValue(new ArrayList<>(waypoints));
    }

    private void onEntryChanged() {
        persistModel();
        structureDirty = true;
    }

    private void onEntryBlurred() {
        structureDirty = true;
    }

    private void requestRemove(WaypointEntry entry) {
        pendingRemoval.add(entry);
        structureDirty = true;
    }

    private void reconcile() {
        boolean deferred = false;

        List<WaypointEntry> stillPending = new ArrayList<>();
        for (WaypointEntry entry : pendingRemoval) {
            if (entry.isStationFocused()) {
                stillPending.add(entry);
                deferred = true;
            } else {
                removeComponent(entry);
            }
        }
        pendingRemoval.clear();
        pendingRemoval.addAll(stillPending);

        List<WaypointEntry> current = entries();
        for (int i = 0; i < current.size(); i++) {
            WaypointEntry entry = current.get(i);
            boolean isLast = i == current.size() - 1;
            if (!isLast && entry.getStationText().trim().isBlank() && !entry.isStationFocused()) {
                removeComponent(entry);
            }
        }

        current = entries();
        boolean roomForMore = current.size() < MAX_WAYPOINTS;
        boolean lastIsFilled = !current.isEmpty() && !current.get(current.size() - 1).getStationText().trim().isBlank();
        if (current.isEmpty() || (roomForMore && lastIsFilled)) {
            addComponent(new WaypointEntry(null));
        }

        persistModel();
        updateRowStates();
        applySize();

        if (deferred) {
            structureDirty = true;
        }
    }

    private void updateRowStates() {
        List<WaypointEntry> current = entries();
        for (int i = 0; i < current.size(); i++) {
            WaypointEntry entry = current.get(i);
            boolean isLast = i == current.size() - 1;
            boolean placeholder = isLast && entry.getStationText().trim().isBlank();
            entry.setDeletable(!placeholder);
        }
    }

    private void applySize() {
        int rows = Math.max(1, componentsCount());
        setHeight(rows * (WaypointEntry.HEIGHT + GAP) - GAP + BORDER * 2);
        if (host instanceof SettingFlyout f) {
            f.resizeToContent(this);
        }
    }

    private class WaypointEntry extends DLGuiComponent {

        private static final int ROW_GAP = 2;
        public static final int HEIGHT = FlatIconButton.HEIGHT;

        private final FlatIconButton deleteBtn;
        private final CreateTextBox stationBox;
        private final CreateTimeSelectionComponent waitingTime;
        private boolean suppressEvents = false;

        private WaypointEntry(Waypoint data) {
            super(0, 0, 1, HEIGHT);

            FlowLayout layout = new FlowLayout();
            layout.wrap.set(false);
            layout.horizontalGap.set(ROW_GAP);
            this.layout.set(layout);

            deleteBtn = addComponent(new FlatIconButton(0, 0, ModGuiIcons.DELETE.getAsSprite(16, 16)));
            deleteBtn.layoutContraint.set(FlowLayout.FlowConstraint.END);
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                requestRemove(this);
                return false;
            });

            waitingTime = addComponent(new CreateTimeSelectionComponent(0, 0, 50));
            waitingTime.layoutContraint.set(FlowLayout.FlowConstraint.END);
            waitingTime.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
                if (!suppressEvents) {
                    onEntryChanged();
                }
                return false;
            });

            stationBox = addComponent(new CreateTextBox(0, 0, 1));
            stationBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
            stationBox.addEventListener(DLRichTextLabel.TextChangedEvent.class, (s, e) -> {
                if (!suppressEvents) {
                    onEntryChanged();
                }
                return false;
            });
            stationBox.addEventListener(DLGuiStandardEvents.FocusChangedEvent.class, (s, e) -> {
                if (!e.focus() && !suppressEvents) {
                    onEntryBlurred();
                }
                return false;
            });

            suppressEvents = true;
            if (data != null) {
                stationBox.text.get().set(data.station());
                waitingTime.value.set((double) data.minStay());
            }
            stationBox.autocompleteManager.set(new StationTagsAutocomplete());
            suppressEvents = false;
        }

        private String getStationText() {
            return stationBox.text.get().getPlainText();
        }

        private long getMinStay() {
            return waitingTime.value.get().longValue();
        }

        private boolean isStationFocused() {
            return stationBox.isFocused();
        }

        private void setDeletable(boolean deletable) {
            deleteBtn.visible.set(deletable);
        }

        private void closeAutocomplete() {
            stationBox.autocompleteManager.set(null);
        }
    }
}
