package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.SimpleStaticTextDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.util.VariableManager;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERStaticText implements AbstractAdvancedDisplayRenderer<SimpleStaticTextDisplaySettings> {


    private final BERLabel label = new BERLabel()        
        .setScrollingSpeed(2)
        .setYScale(0.75f)
        .setScale(0.75f, 0.5f)
        .setPos(3, 5.2f)
        .setCentered(true)
        .setText(TextUtils.empty())
    ;

    @Override
    public void renderTick(float deltaTime) {
        label.renderTick();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        MutableComponent text = getText(getDisplaySettings(blockEntity).getStaticText());    
        label.setText(text);
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
        
        MutableComponent text = getText(getDisplaySettings(blockEntity).getStaticText());

        label
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
            .setText(text)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 3 - label.getX(), BoundsHitReaction.SCALE_SCROLL)
        ;
    }
}
