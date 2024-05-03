package de.mrjulsen.crn.registry;

import java.util.function.Supplier;

import com.simibubi.create.CreateClient;
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
import de.mrjulsen.crn.block.TrainStationClockBlock;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ClientWrapper {

    public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY_ALL = ClientWrapper.getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display", "advanced_display");

    public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY = ClientWrapper.getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display", "advanced_display");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY = ClientWrapper.getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display", "advanced_display");

	public static final CTSpriteShiftEntry CT_ADVANCED_DISPLAY_SMALL = ClientWrapper.getCT(AllCTTypes.OMNIDIRECTIONAL, "advanced_display_small", "advanced_display_small");
    public static final CTSpriteShiftEntry CT_HORIZONTAL_ADVANCED_DISPLAY_SMALL = ClientWrapper.getCT(AllCTTypes.HORIZONTAL_KRYPPERS, "advanced_display_small", "advanced_display_small");


	@Environment(EnvType.CLIENT)
	public static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
    
	public static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
		return CTSpriteShifter.getCT(type, new ResourceLocation(ExampleMod.MOD_ID, "block/" + blockTextureName),
			new ResourceLocation(ExampleMod.MOD_ID, "block/" + connectedTextureName + "_connected"));
	}
}
