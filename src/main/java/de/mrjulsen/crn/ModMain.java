package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;

import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.proxy.ClientInitWrapper;
import de.mrjulsen.crn.proxy.ServerInit;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.registry.ModCreativeModeTab;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.crn.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nullable;

import org.slf4j.Logger;

@Mod(ModMain.MOD_ID)
public final class ModMain {

    public static final String MOD_ID = "createrailwaysnavigator";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    @Nullable
    public static KineticStats create(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof AdvancedDisplayBlock) {
                return new KineticStats(block);
            }
        }
        return null;
    }

	static {
        ModMain.REGISTRATE.setTooltipModifierFactory(item ->
            new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(ModMain.create(item)))
        );
    }

    public ModMain() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(ServerInit::setup);
        eventBus.addListener(ClientInitWrapper::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, MOD_ID + "-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, MOD_ID + "-common.toml");

		REGISTRATE.registerEventListeners(eventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, MOD_ID + "-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, MOD_ID + "-common.toml");
        
        new ModCreativeModeTab("createrailwaysnavigatortab");
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        NetworkManager.create();
        ModExtras.register();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
