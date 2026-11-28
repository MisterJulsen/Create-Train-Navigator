/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.forge.client;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class ClientWrapperForge {

    public static NonNullFunction<BakedModel, ? extends BakedModel> copycatModel(ConnectedTextureBehaviour ctDisplay, ConnectedTextureBehaviour ctFrame, ResourceLocation copycatTexture) {
        return (model) -> new CopycatDisplayModel(model, ctDisplay, ctFrame, copycatTexture);
    }
}
