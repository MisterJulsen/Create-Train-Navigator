package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.event.CRNClientEventsRegistryEvent;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.ModClientEvents;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.registry.*;
import de.mrjulsen.mcdragonlib.net.DLNetworkManager;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

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
			return new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		}).defaultCreativeTab(MOD_ID, builder -> builder
            .title(TextUtils.text("Create Railways Navigator"))
            .icon(() -> new ItemStack(ModItems.NAVIGATOR.get()))
        ).build();
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
        ModAccessorTypes.init();
        ModTrainStatusInfos.init();
        ModDisplayTypes.init();
        ModDataComponents.init();

        DLNetworkManager.registerPackets(MOD_ID, List.of(
            AdvancedDisplayUpdatePacket.class
        ), List.of(
            ServerErrorPacket.class
        ));
        
        CRNPlatformSpecific.registerConfig();

        ModCommonEvents.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModClientEvents.init();
        }

        CRNEventsManager.getEvent(CRNClientEventsRegistryEvent.class).register(MOD_ID, () -> {
        });

    }

    public static boolean isDebug() {
        return Platform.isDevelopmentEnvironment();
    }
}
