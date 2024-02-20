package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.widgets.AliasEntryWidget;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

public class AliasSettingsScreen extends AbstractEntryListSettingsScreen<TrainStationAlias, AliasEntryWidget> {
    public AliasSettingsScreen(Level level, Screen lastScreen) {
        super(level, lastScreen, Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.title"));
    }

    @Override
    protected AliasEntryWidget createWidget(WidgetCreationData<TrainStationAlias, AbstractEntryListSettingsScreen<TrainStationAlias, AliasEntryWidget>> widgetData, TrainStationAlias data) {
        Collection<String> expandedAliasNames = widgetData.previousEntries().stream().filter(x -> x instanceof AliasEntryWidget && ((AliasEntryWidget)x).isExpanded()).map(x -> ((AliasEntryWidget)x).getAlias().getAliasName().get()).toList();
        return new AliasEntryWidget(widgetData.parent(), widgetData.x(), widgetData.y(), data, () -> refreshEntries(), expandedAliasNames.contains(data.getAliasName().get()));
    }

    @Override
    protected TrainStationAlias[] getData(String searchText) {
        return GlobalSettingsManager.getInstance().getSettingsData().getAliasList().stream().filter(x -> x.getAliasName().get().toLowerCase().contains(searchText.toLowerCase())).toArray(TrainStationAlias[]::new);
    }

    @Override
    protected void onCreateNewEntry(String value, Runnable refreshAction) {
        GlobalSettingsManager.getInstance().getSettingsData().registerAlias(new TrainStationAlias(AliasName.of(value)), refreshAction);
    }
}
