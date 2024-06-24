package de.mrjulsen.crn.registry;

import java.util.function.Supplier;

import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.block.AdvancedDisplayBoardBlock;
import de.mrjulsen.crn.block.AdvancedDisplayHalfPanelBlock;
import de.mrjulsen.crn.block.AdvancedDisplayPanelBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySlopedBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySmallBlock;
import de.mrjulsen.crn.block.TrainStationClockBlock;
import de.mrjulsen.crn.block.connected.AdvancedDisplayCTBehaviour;
import de.mrjulsen.crn.block.connected.AdvancedDisplaySmallCTBehaviour;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public class ModBlocks {	

	static {
		CreateRailwaysNavigator.REGISTRATE.creativeModeTab(() -> ModCreativeModeTab.MAIN);
	}

	public static final BlockEntry<AdvancedDisplayBlock> ADVANCED_DISPLAY_BLOCK = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_block", AdvancedDisplayBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY_ALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

    public static final BlockEntry<AdvancedDisplayBoardBlock> ADVANCED_DISPLAY = CreateRailwaysNavigator.REGISTRATE.block("advanced_display", AdvancedDisplayBoardBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplaySmallBlock> ADVANCED_DISPLAY_SMALL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_small", AdvancedDisplaySmallBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, ClientWrapper.CT_ADVANCED_DISPLAY_SMALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplayPanelBlock> ADVANCED_DISPLAY_PANEL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_panel", AdvancedDisplayPanelBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(ClientWrapper.CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplayHalfPanelBlock> ADVANCED_DISPLAY_HALF_PANEL = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_half_panel", AdvancedDisplayHalfPanelBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, ClientWrapper.CT_ADVANCED_DISPLAY_SMALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplaySlopedBlock> ADVANCED_DISPLAY_SLOPED = CreateRailwaysNavigator.REGISTRATE.block("advanced_display_sloped", AdvancedDisplaySlopedBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(ClientWrapper.CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, ClientWrapper.CT_ADVANCED_DISPLAY_SMALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();	
	
    public static final BlockEntry<TrainStationClockBlock> TRAIN_STATION_CLOCK = CreateRailwaysNavigator.REGISTRATE.block("train_station_clock", TrainStationClockBlock::new)
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.build()
		.register();

	public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
		Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> onClient(() -> () -> ClientWrapper.registerCTBehviour(entry, behavior));
	}

	protected static void onClient(Supplier<Runnable> toRun) {
		EnvExecutor.runInEnv(Env.CLIENT, toRun);
	}

    public static void register() {
    }
}
