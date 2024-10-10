package de.mrjulsen.crn.forge;

import java.util.function.Supplier;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;

@Mod(CreateRailwaysNavigator.MOD_ID)
public class CRNPlatformSpecificClientImpl {
    
    public static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
}
