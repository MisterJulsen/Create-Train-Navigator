package de.mrjulsen.crn.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.TrainStationClockBlockEntity;
import de.mrjulsen.mcdragonlib.client.ber.StaticBlockEntityRenderer;

public class ModBlockEntities {
	
    public static final BlockEntityEntry<AdvancedDisplayBlockEntity> ADVANCED_DISPLAY_BLOCK_ENTITY = CreateRailwaysNavigator.REGISTRATE
		.blockEntity("advanced_display_block_entity", AdvancedDisplayBlockEntity::new)
		.validBlocks(
			ModBlocks.ADVANCED_DISPLAY,
			ModBlocks.ADVANCED_DISPLAY_BLOCK,	
			ModBlocks.ADVANCED_DISPLAY_PANEL,
			ModBlocks.ADVANCED_DISPLAY_HALF_PANEL,
			ModBlocks.ADVANCED_DISPLAY_SMALL,
			ModBlocks.ADVANCED_DISPLAY_SLOPED
		)
		.renderer(() -> StaticBlockEntityRenderer::new)
		.register();

	public static final BlockEntityEntry<TrainStationClockBlockEntity> TRAIN_STATION_CLOCK_BLOCK_ENTITY = CreateRailwaysNavigator.REGISTRATE
		.blockEntity("train_station_clock_block_entity", TrainStationClockBlockEntity::new)
		.validBlocks(
			ModBlocks.TRAIN_STATION_CLOCK
		)
		.renderer(() -> StaticBlockEntityRenderer::new)
		.register();

    public static void init() {
    } 
}
