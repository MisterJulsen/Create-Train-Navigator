/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.forge;

import java.util.function.Supplier;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.level.block.Block;

public class CRNPlatformSpecificClientImpl {
    
    public static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CatnipServices.REGISTRIES.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
}
