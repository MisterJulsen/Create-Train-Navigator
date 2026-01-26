package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.SimpleStaticTextDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.util.VariableManager;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERStaticText implements AbstractAdvancedDisplayRenderer<SimpleStaticTextDisplaySettings> {


    private final BERLabel label = new BERLabel();

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
        MutableComponent text = getText(getDisplaySettings(blockEntity).getStaticText());    
        label.text.set(text);
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics, light);
    }

    private MutableComponent getText(String input) {
        String staticText = VariableManager.replacePlaceholders(input);
        MutableComponent text = TextUtils.empty();
        if (staticText != null) {
            try {
                text = Component.Serializer.fromJson(staticText);
            } catch (Exception e) {
                text = TextUtils.text(staticText);
            }
        }
        if (text == null) {
            text = TextUtils.empty();
        }
        return text;
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
