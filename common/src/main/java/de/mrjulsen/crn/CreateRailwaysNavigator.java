package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.event.ModClientEvents;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.registry.ModCreativeModeTab;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.registry.ModSchedule;
import de.mrjulsen.crn.registry.ModTrainStatusInfos;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import org.slf4j.Logger;

public final class CreateRailwaysNavigator {

    public static final String MOD_ID = "createrailwaysnavigator";
    public static final String SHORT_MOD_ID = "crn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String DISCORD = "https://discord.mrjulsen.net";
    public static final String GITHUB = "https://github.com/MisterJulsen/Create-Train-Navigator";
    
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
		REGISTRATE.setTooltipModifierFactory(item -> {
			return new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		});
	}

    public static KineticStats create(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof AdvancedDisplayBlock) {
                return new KineticStats(block);
            }
        }
        return null;
    }
    

    public static void load() {}

    public static void init() {
        
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModExtras.init();
        ModSchedule.init();
        ModNetworkManager.init();
        ModTrainStatusInfos.init();
        ModDisplayTypes.init();
        ModCreativeModeTab.setup();
        
        CRNPlatformSpecific.registerConfig();

        ModCommonEvents.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModClientEvents.init();
        }

    }

    public static boolean isDebug() {
        return Platform.isDevelopmentEnvironment();
    }
}
