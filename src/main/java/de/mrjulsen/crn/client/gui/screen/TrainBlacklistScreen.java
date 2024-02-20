package de.mrjulsen.crn.client.gui.screen;

import java.util.Collection;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

public class TrainBlacklistScreen extends AbstractBlacklistScreen {

    public TrainBlacklistScreen(Level level, Screen lastScreen) {
        super(level, lastScreen, Utils.translate("gui." + ModMain.MOD_ID + ".train_blacklist.title"));
    }

    @Override
    protected Collection<String> getSuggestions() {
        return ClientTrainStationSnapshot.getInstance().getAllTrainNames();
    }

    @Override
    protected boolean checkIsBlacklisted(String entry) {
        return GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(entry);
    }

    @Override
    protected String[] getBlacklistedNames(String searchText) {
        return GlobalSettingsManager.getInstance().getSettingsData().getTrainBlacklist().stream().filter(x -> x.toLowerCase().contains(searchText.toLowerCase())).toArray(String[]::new);
    }

    @Override
    protected void addToBlacklist(String name, Runnable andThen) {
        if (GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(name) || ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream().noneMatch(x -> x.equals(name))) {
            return;
        }

        GlobalSettingsManager.getInstance().getSettingsData().addTrainToBlacklist(name, andThen);
    }

    @Override
    protected void removeFromBlacklist(String name, Runnable andThen) {
        GlobalSettingsManager.getInstance().getSettingsData().removeTrainFromBlacklist(name, andThen);
    }
    
}
