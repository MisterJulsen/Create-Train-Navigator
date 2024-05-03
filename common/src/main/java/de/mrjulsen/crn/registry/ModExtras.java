package de.mrjulsen.crn.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModExtras {

    public static boolean registeredTrackStationSource = false;

    public static void register() {
        Block maybeRegistered = null;

        try {
            maybeRegistered = AllBlocks.TRACK_STATION.get();
            if (maybeRegistered == Blocks.AIR) {
                throw new NullPointerException();
            }
        } catch (NullPointerException ignored) {
            maybeRegistered = null;
        }
        
		Create.REGISTRATE.addRegisterCallback("track_station", Registry.BLOCK_REGISTRY, ModExtras::addDisplaySource);
        if (maybeRegistered != null) {
            addDisplaySource(maybeRegistered);
        }
    }

	public static void addDisplaySource(Block block) {
        if (registeredTrackStationSource) return;
        CreateRailwaysNavigator.LOGGER.info("Custom display sources registered!");
        AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplaySource()).accept(block);
        registeredTrackStationSource = true;
    }
}
