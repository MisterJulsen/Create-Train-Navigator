package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

public class StationBlacklistScreen extends AbstractBlacklistScreen {

    public StationBlacklistScreen(Level level, Screen lastScreen) {
        super(level, lastScreen, Utils.translate("gui." + ModMain.MOD_ID + ".blacklist.title"));
    }

    @Override
    protected Collection<String> getSuggestions() {
        return ClientTrainStationSnapshot.getInstance().getAllTrainStations();
    }

    @Override
    protected boolean checkIsBlacklisted(String entry) {
        return GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(entry);
    }

    @Override
    protected String[] getBlacklistedNames(String searchText) {
        return GlobalSettingsManager.getInstance().getSettingsData().getBlacklist().stream().filter(x -> x.toLowerCase().contains(searchText.toLowerCase())).toArray(String[]::new);
    }

    @Override
    protected void addToBlacklist(String name, Runnable andThen) {
        if (GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(name) || ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream().noneMatch(x -> x.toLowerCase().equals(name.toLowerCase()))) {
            return;
        }

        GlobalSettingsManager.getInstance().getSettingsData().addToBlacklist(name, andThen);
    }

    @Override
    protected void removeFromBlacklist(String name, Runnable andThen) {
        GlobalSettingsManager.getInstance().getSettingsData().removeFromBlacklist(name, andThen);
    }
    
}
