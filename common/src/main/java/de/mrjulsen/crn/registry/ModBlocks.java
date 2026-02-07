package de.mrjulsen.crn.registry;

import java.util.function.Supplier;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.*;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayInteractionBehaviour;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayMovementBehaviour;
import de.mrjulsen.crn.block.connected.AdvancedDisplayCTBehaviour;
import de.mrjulsen.crn.block.connected.AdvancedDisplaySmallCTBehaviour;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModBlocks {	

	public static final BlockEntry<AdvancedDisplayBlock> ADVANCED_DISPLAY_BLOCK = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_block", AdvancedDisplayBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL),
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
	public static final BlockEntry<AdvancedDisplaySlabBlock> ADVANCED_DISPLAY_SLAB = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_slab", AdvancedDisplaySlabBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL),
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

    public static final BlockEntry<AdvancedDisplayBoardBlock> ADVANCED_DISPLAY = CreateRailwaysNavigator.REGISTRATE.block("advanced_display", AdvancedDisplayBoardBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL),
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<AdvancedDisplaySmallBlock> ADVANCED_DISPLAY_SMALL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_small", AdvancedDisplaySmallBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL),
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<AdvancedDisplayPanelBlock> ADVANCED_DISPLAY_PANEL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_panel", AdvancedDisplayPanelBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL),
					new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<AdvancedDisplayHalfPanelBlock> ADVANCED_DISPLAY_HALF_PANEL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_half_panel", AdvancedDisplayHalfPanelBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.transform(BuilderTransformer.copycatDisplay(
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL),
					new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL_BORDER),
					new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "block/advanced_display_back")))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<AdvancedDisplaySlopedBlock> ADVANCED_DISPLAY_SLOPED = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_sloped", AdvancedDisplaySlopedBlock::new)
			.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, ClientWrapper.CT_ADVANCED_DISPLAY_SMALL)))
			.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL_BORDER, ClientWrapper.CT_ADVANCED_DISPLAY_SMALL_BORDER)))
			.addLayer(() -> RenderType::cutout)
			.color(() -> AbstractAdvancedDisplayBlock::getDisplayColor)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.transform(DisplayTarget.displayTarget(ModExtras.ADVANCED_DISPLAY_BOARD_TARGET))
			.onRegister(MovementBehaviour.movementBehaviour(new AdvancedDisplayMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new AdvancedDisplayInteractionBehaviour()))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
	
    public static final BlockEntry<TrainStationClockBlock> TRAIN_STATION_CLOCK = CreateRailwaysNavigator.REGISTRATE.block("train_station_clock", TrainStationClockBlock::new)
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<NavigatorLecternBlock> NAVIGATOR_LECTERN = CreateRailwaysNavigator.REGISTRATE.block("navigator_lectern", NavigatorLecternBlock::new)
			.initialProperties(() -> Blocks.LECTERN)
			.transform(TagGen.axeOnly())
			.loot((lt, block) -> lt.dropOther(block, Blocks.LECTERN))
			.register();

	public static <T extends Block> NonNullConsumer<? super T> connectedTextures(Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> onClient(() -> () -> ClientWrapper.registerCTBehviour(entry, behavior));
	}

	protected static void onClient(Supplier<Runnable> toRun) {
		EnvExecutor.runInEnv(Env.CLIENT, toRun);
	}

    public static void init() {
    }
}
