package de.mrjulsen.crn.fabric.client;

import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.resources.model.BakedModel;

public class WrappedCTModel extends CTModel {
    public WrappedCTModel(BakedModel originalModel, ConnectedTextureBehaviour behaviour) {
        super(originalModel, behaviour);
    }

}
