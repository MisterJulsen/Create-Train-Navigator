package de.mrjulsen.crn.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;

import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;

public class ModExtras {

    public static boolean registeredTrackStationSource = false;

    public static void register() {
        Block maybeRegistered = null;
        try {
            maybeRegistered = AllBlocks.TRACK_STATION.get();
        } catch (NullPointerException ignored) {
            maybeRegistered = null;
        }
		Create.REGISTRATE.addRegisterCallback("track_station", Registry.BLOCK_REGISTRY, ModExtras::addSignalSource);
        if (maybeRegistered != null) {
            addSignalSource(maybeRegistered);
        }        
    }

	public static void addSignalSource(Block block) {
        if (registeredTrackStationSource) return;
        AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplaySource()).accept(block);
        registeredTrackStationSource = true;
    }
}
