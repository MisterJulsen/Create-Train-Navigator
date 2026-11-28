/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.fabric.client;

import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.resources.model.BakedModel;

public class WrappedCTModel extends CTModel {
    public WrappedCTModel(BakedModel originalModel, ConnectedTextureBehaviour behaviour) {
        super(originalModel, behaviour);
    }

}
