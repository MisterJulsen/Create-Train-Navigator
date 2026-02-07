package de.mrjulsen.crn.forge.client;

import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class WrappedCTModel extends CTModel {
    public WrappedCTModel(BakedModel originalModel, ConnectedTextureBehaviour behaviour) {
        super(originalModel, behaviour);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        ModelData.Builder b = super.gatherModelData(builder, world, pos, state, blockEntityData);
        for (ModelProperty<?> v : blockEntityData.getProperties()) {
            b.with((ModelProperty<? super Object>) v, (Object)blockEntityData.get(v));
        }
        return b;
    }
}
