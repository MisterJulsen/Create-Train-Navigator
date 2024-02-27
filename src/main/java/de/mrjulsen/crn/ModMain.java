package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.item.ModItems;
import de.mrjulsen.crn.item.creativemodetab.ModCreativeModeTab;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.proxy.ClientInitWrapper;
import de.mrjulsen.crn.proxy.ServerInit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

@Mod(ModMain.MOD_ID)
public final class ModMain {

    // The value here should match an entry in the META-INF/mods.toml file
    public static final String MOD_ID = "createrailwaysnavigator";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModMain() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(ServerInit::setup);
        eventBus.addListener(ClientInitWrapper::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, MOD_ID + "-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, MOD_ID + "-common.toml");

        ModItems.register(eventBus);
        ModCreativeModeTab.register(eventBus);
        NetworkManager.create();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
