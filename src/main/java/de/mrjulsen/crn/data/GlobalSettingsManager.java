package de.mrjulsen.crn.data;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID)
public class GlobalSettingsManager extends SavedData {

    private static final String FILE_ID = ModMain.MOD_ID + "_global_settings";
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
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                // execute on client             
                instance = new GlobalSettingsManager();
            } else {
                // execute on server
                ModMain.LOGGER.debug("Create Instance");
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
        NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new GlobalSettingsRequestPacket(id));
    }
}
