package de.mrjulsen.crn.registry;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;

public class BuilderTransformer {

    @ExpectPlatform
    public static <B extends AbstractAdvancedDisplayBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycatDisplay(ConnectedTextureBehaviour behaviour, ConnectedTextureBehaviour behaviour2, ResourceLocation copycatTexture) {
        throw new AssertionError();
    }
}
