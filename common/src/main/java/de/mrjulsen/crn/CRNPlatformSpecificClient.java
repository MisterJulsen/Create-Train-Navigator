/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn;

import java.util.function.Supplier;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.block.Block;

public class CRNPlatformSpecificClient {

    @ExpectPlatform
    public static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
        throw new AssertionError();
	}   
}
