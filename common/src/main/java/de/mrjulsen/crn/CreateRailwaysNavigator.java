package de.mrjulsen.crn;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.event.ClientEvents;
import de.mrjulsen.crn.event.ModEvents;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacket;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsUpdatePacket;
import de.mrjulsen.crn.network.packets.cts.NavigationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NearestStationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrackStationsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NavigationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NearestStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.NextConnectionsResponsePacket;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.network.packets.stc.TimeCorrectionPacket;
import de.mrjulsen.crn.network.packets.stc.TrackStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.TrainDataResponsePacket;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.mcdragonlib.net.NetworkManagerBase;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import javax.annotation.Nullable;

import org.slf4j.Logger;

public final class CreateRailwaysNavigator {

    public static final String MOD_ID = "createrailwaysnavigator";
    public static final Logger LOGGER = LogUtils.getLogger();
    
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
           
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();        
        ModExtras.register();
        
        crnNet = new NetworkManagerBase(MOD_ID, "crn_network", List.of(
            // cts
            GlobalSettingsRequestPacket.class,
            GlobalSettingsUpdatePacket.class,
            NavigationRequestPacket.class,
            NearestStationRequestPacket.class,
            NextConnectionsRequestPacket.class,
            RealtimeRequestPacket.class,
            TrackStationsRequestPacket.class,
            TrainDataRequestPacket.class,
            AdvancedDisplayUpdatePacket.class,

            // stc
            GlobalSettingsResponsePacket.class,
            NavigationResponsePacket.class,
            NearestStationResponsePacket.class,
            NextConnectionsResponsePacket.class,
            RealtimeResponsePacket.class,
            ServerErrorPacket.class,
            TrackStationResponsePacket.class,
            TrainDataResponsePacket.class,
            TimeCorrectionPacket.class
        ));
        
        CRNPlatformSpecific.registerConfig();

        ModEvents.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ClientEvents.init();
        }
    }

    public static NetworkManagerBase net() {
        return crnNet;
    }
}
