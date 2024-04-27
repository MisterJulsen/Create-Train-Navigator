package de.mrjulsen.crn.client.ber.base;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface IBERInstance<T extends BlockEntity> {
    IBlockEntityRendererInstance<T> getRenderer();
}
