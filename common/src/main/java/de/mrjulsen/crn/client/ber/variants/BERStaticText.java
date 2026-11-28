/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.SimpleStaticTextDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.CachedDisplayText;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERStaticText implements AbstractAdvancedDisplayRenderer<SimpleStaticTextDisplaySettings> {


    private final BERLabel label = new BERLabel();
    private final CachedDisplayText text = new CachedDisplayText();

    public BERStaticText() {
        label.position.set(Point.of(3, 5.2f));
        label.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        label.horizontalMinScale.set(0.5f);
        label.horizontalMaxScale.set(0.75f);
        label.verticalMinScale.set(0.75f);
        label.verticalMaxScale.set(0.75f);
        label.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        if (!blockEntity.isController()) {
            return;
        }
        text.applyTo(label, getDisplaySettings(blockEntity).getStaticText(), blockEntity);
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics, light);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (!blockEntity.isController()) {
            return;
        }        
        
        label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));
        label.color.set(getDisplaySettings(blockEntity).getFontColor());
        label.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - 3 - label.x.get());
        label.horizontalAlign.set(ETextAlignment.CENTER);
    }
}
