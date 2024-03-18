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
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.AdvancedDisplayBlock;
import de.mrjulsen.crn.block.AdvancedDisplayPanelBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySmallBlock;
import de.mrjulsen.crn.block.connected.RightLeftCTBehaviour;
import de.mrjulsen.crn.block.display.StationDisplayTarget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

public class ModBlocks {	

	static {
		ModMain.REGISTRATE.creativeModeTab(() -> ModCreativeModeTab.MAIN);
	}

    public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY = getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display", "advanced_display");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY = getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display", "advanced_display");

	public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY_SMALL = getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display_small", "advanced_display_small");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL = getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display_small", "advanced_display_small");

    public static final BlockEntry<AdvancedDisplayBlock> ADVANCED_DISPLAY = ModMain.REGISTRATE.block("advanced_display", AdvancedDisplayBlock::new)
		.onRegister(connectedTextures(() -> new RightLeftCTBehaviour(CT_HORIZONTAL_ADVANCED_DISPLAY, CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(Material.METAL)
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new StationDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplaySmallBlock> ADVANCED_DISPLAY_SMALL = ModMain.REGISTRATE.block("advanced_display_small", AdvancedDisplaySmallBlock::new)
		.onRegister(connectedTextures(() -> new RightLeftCTBehaviour(CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL, CT_ADVANCED_DISPLAY_SMALL)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(Material.METAL)
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new StationDisplayTarget()))
		.item()
		.build()
		.register();

	public static final BlockEntry<AdvancedDisplayPanelBlock> ADVANCED_DISPLAY_PANEL = ModMain.REGISTRATE.block("advanced_display_panel", AdvancedDisplayPanelBlock::new)
		.onRegister(connectedTextures(() -> new RightLeftCTBehaviour(CT_HORIZONTAL_ADVANCED_DISPLAY, CT_ADVANCED_DISPLAY)))
		.addLayer(() -> RenderType::cutout)
		.initialProperties(Material.METAL)
		.onRegister(AllDisplayBehaviours.assignDataBehaviour(new StationDisplayTarget()))
		.item()
		.build()
		.register();

    public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
		Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> onClient(() -> () -> registerCTBehviour(entry, behavior));
	}

	protected static void onClient(Supplier<Runnable> toRun) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, toRun);
	}

    @OnlyIn(Dist.CLIENT)
	private static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
    
	private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
		return CTSpriteShifter.getCT(type, new ResourceLocation(ModMain.MOD_ID, "block/" + blockTextureName),
			new ResourceLocation(ModMain.MOD_ID, "block/" + connectedTextureName + "_connected"));
	}

    public static void register() {
    }
}
