package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.event.CRNClientEventsRegistryEvent;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.ModClientEvents;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.registry.ModCreativeModeTab;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.crn.registry.ModSchedule;
import de.mrjulsen.crn.registry.ModTrainStatusInfos;
import de.mrjulsen.mcdragonlib.net.NetworkManagerBase;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import javax.annotation.Nullable;

import org.slf4j.Logger;

public final class CreateRailwaysNavigator {

    public static final String MOD_ID = "createrailwaysnavigator";
    public static final String SHORT_MOD_ID = "crn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String DISCORD = "https://discord.gg/hH7YxTrPpk";
    public static final String GITHUB = "https://github.com/MisterJulsen/Create-Train-Navigator";
    
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
		REGISTRATE.setTooltipModifierFactory(item -> {
			return new ItemDescription.Modifier(item, Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		});
	}

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

    private static NetworkManagerBase crnNet;

    

    public static void load() {}

    public static void init() {
           
        CRNPlatformSpecific.registerConfig();
        
        if (Platform.getEnv() == EnvType.CLIENT) {
            ModKeys.init();
        }
        ModCreativeModeTab.setup();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();        
        ModExtras.init();
        ModSchedule.init();
        ModAccessorTypes.init();
        ModTrainStatusInfos.init();
        ModDisplayTypes.init();
        
        crnNet = new NetworkManagerBase(MOD_ID, "crn_network", List.of(
            // cts
            AdvancedDisplayUpdatePacket.class,

            // stc
            ServerErrorPacket.class
        ));
        

        ModCommonEvents.init();
        if (Platform.getEnv() == EnvType.CLIENT) {
            ModClientEvents.init();
        }

        CRNEventsManager.getEvent(CRNClientEventsRegistryEvent.class).register(MOD_ID, () -> {
        });

    }

    public static NetworkManagerBase net() {
        return crnNet;
    }

    public static boolean isDebug() {
        return Platform.isDevelopmentEnvironment();
    }
}
