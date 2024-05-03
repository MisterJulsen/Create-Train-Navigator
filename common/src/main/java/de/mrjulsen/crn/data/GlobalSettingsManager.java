package de.mrjulsen.crn.data;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsRequestPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class GlobalSettingsManager extends SavedData {

    private static final String FILE_ID = CreateRailwaysNavigator.MOD_ID + "_global_settings";
    private static volatile GlobalSettingsManager instance;
    
    private GlobalSettings settingsData;

    private GlobalSettingsManager() {
        settingsData = new GlobalSettings();
    }
    
    private GlobalSettingsManager(GlobalSettings settings) {
        instance = this;
        settingsData = settings;
    }
    

    public static GlobalSettingsManager createClientInstance() {
        if (instance == null) {
            instance = new GlobalSettingsManager();
        }
        return instance;
    }

    public static GlobalSettingsManager getInstance() {
        if (instance == null) {
            MinecraftServer server = CRNPlatformSpecific.getServer();
            if (server == null) {
                // execute on client             
                instance = new GlobalSettingsManager();
            } else {
                // execute on server
                CreateRailwaysNavigator.LOGGER.debug("Create Instance");
                ServerLevel level = server.overworld();
                instance = level.getDataStorage().computeIfAbsent(GlobalSettingsManager::load, GlobalSettingsManager::new, FILE_ID);
            }
        }
        return instance;
    }
    
    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        return settingsData.toNbt(pCompoundTag);
    }

    private static GlobalSettingsManager load(CompoundTag tag) {
        GlobalSettings settings = GlobalSettings.fromNbt(tag);
        return new GlobalSettingsManager(settings);

    }

    public final GlobalSettings getSettingsData() {
        return settingsData;
    }

    public void updateSettingsData(GlobalSettings settings) {
        this.settingsData = settings;
        setDirty();
    }

    public static void syncToClient(Runnable then) {
        long id = InstanceManager.registerClientResponseReceievedAction(then);
        CreateRailwaysNavigator.net().CHANNEL.sendToServer(new GlobalSettingsRequestPacket(id));
    }
}
