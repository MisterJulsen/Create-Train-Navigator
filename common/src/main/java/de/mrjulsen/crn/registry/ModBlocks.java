package de.mrjulsen.crn.registry;

import java.util.function.Supplier;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.block.AdvancedDisplayBoardBlock;
import de.mrjulsen.crn.block.AdvancedDisplayPanelBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySmallBlock;
import de.mrjulsen.crn.block.TrainStationClockBlock;
import de.mrjulsen.crn.block.connected.AdvancedDisplayCTBehaviour;
import de.mrjulsen.crn.block.connected.AdvancedDisplaySmallCTBehaviour;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModBlocks {	

	static {
		ExampleMod.REGISTRATE.creativeModeTab(() -> ModCreativeModeTab.MAIN);
	}

	public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY_ALL = getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display", "advanced_display");

    public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY = getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display", "advanced_display");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY = getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display", "advanced_display");

	public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY_SMALL = getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display_small", "advanced_display_small");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL = getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display_small", "advanced_display_small");

	public static final BlockEntry<AdvancedDisplayBlock> ADVANCED_DISPLAY_BLOCK = ExampleMod.REGISTRATE.block("advanced_display_block", AdvancedDisplayBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(CT_ADVANCED_DISPLAY_ALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

    public static final BlockEntry<AdvancedDisplayBoardBlock> ADVANCED_DISPLAY = ExampleMod.REGISTRATE.block("advanced_display", AdvancedDisplayBoardBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplaySmallBlock> ADVANCED_DISPLAY_SMALL = ExampleMod.REGISTRATE.block("advanced_display_small", AdvancedDisplaySmallBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplaySmallCTBehaviour(CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, CT_ADVANCED_DISPLAY_SMALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplayPanelBlock> ADVANCED_DISPLAY_PANEL = ExampleMod.REGISTRATE.block("advanced_display_panel", AdvancedDisplayPanelBlock::new)
		.onRegister(connectedTextures(() -> new AdvancedDisplayCTBehaviour(CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new AdvancedDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<TrainStationClockBlock> TRAIN_STATION_CLOCK = ExampleMod.REGISTRATE.block("train_station_clock", TrainStationClockBlock::new)
		.addLayer(() -> RenderType::cutout)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.build()
		.register();


    public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
		Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> onClient(() -> () -> registerCTBehviour(entry, behavior));
	}

	protected static void onClient(Supplier<Runnable> toRun) {
		EnvExecutor.runInEnv(EnvType.CLIENT, toRun);
	}

	@Environment(EnvType.CLIENT)
	private static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
    
	private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
		return CTSpriteShifter.getCT(type, new ResourceLocation(ExampleMod.MOD_ID, "block/" + blockTextureName),
			new ResourceLocation(ExampleMod.MOD_ID, "block/" + connectedTextureName + "_connected"));
	}

    public static void register() {
    }
}
