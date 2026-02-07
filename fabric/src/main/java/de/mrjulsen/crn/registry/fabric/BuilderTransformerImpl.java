package de.mrjulsen.crn.registry.fabric;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.fabric.client.CopycatDisplayModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BuilderTransformerImpl {

    public static <B extends AbstractAdvancedDisplayBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycatDisplay(ConnectedTextureBehaviour ctDisplay, ConnectedTextureBehaviour ctFrame, ResourceLocation copycatTexture) {
        return b -> b
                .addLayer(() -> RenderType::solid)
                .addLayer(() -> RenderType::cutout)
                .addLayer(() -> RenderType::cutoutMipped)
                .color(() -> AbstractAdvancedDisplayBlock::getDisplayColor)
                .onRegister(CreateRegistrate.blockModel(() -> (model) -> new CopycatDisplayModel(model, ctDisplay, ctFrame)))
                ;
    }
}
