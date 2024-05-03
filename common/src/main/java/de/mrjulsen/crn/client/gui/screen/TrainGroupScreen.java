package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.TrainGroupEntryWidget;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

public class TrainGroupScreen extends AbstractEntryListSettingsScreen<TrainGroup, TrainGroupEntryWidget> {

    public TrainGroupScreen(Level level, Screen lastScreen) {
        super(level, lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_group_settings.title"));
    }

    @Override
    protected TrainGroup[] getData(String searchText) {
        return GlobalSettingsManager.getInstance().getSettingsData().getTrainGroupsList().stream().filter(x -> x.getGroupName().toLowerCase().contains(searchText.toLowerCase())).toArray(TrainGroup[]::new);
    }

    @Override
    protected TrainGroupEntryWidget createWidget(WidgetCreationData<TrainGroup, AbstractEntryListSettingsScreen<TrainGroup, TrainGroupEntryWidget>> widgetData, TrainGroup data) {
        Collection<String> expandedAliasNames = widgetData.previousEntries().stream().filter(x -> x instanceof TrainGroupEntryWidget w && w.isExpanded()).map(x -> ((TrainGroupEntryWidget)x).getTrainGroup().getGroupName()).toList();
        return new TrainGroupEntryWidget(widgetData.parent(), widgetData.x(), widgetData.y(), data, () -> refreshEntries(), expandedAliasNames.contains(data.getGroupName()));
    }

    @Override
    protected void onCreateNewEntry(String value, Runnable refreshAction) {
        GlobalSettingsManager.getInstance().getSettingsData().registerTrainGroup(new TrainGroup(value), refreshAction);
    }
}
