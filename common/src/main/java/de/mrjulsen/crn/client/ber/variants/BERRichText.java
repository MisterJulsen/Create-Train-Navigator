package de.mrjulsen.crn.client.ber.variants;

import java.util.List;

import com.google.gson.JsonParser;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings.TextComponent;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.util.VariableManager;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERRichText implements AbstractAdvancedDisplayRenderer<StaticTextDisplaySettings> {

    private BERLabel[] labels = new BERLabel[0];

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        for (int i = 0; i < labels.length && i < getDisplaySettings(blockEntity).getComponents().size(); i++) {
            TextComponent component = getDisplaySettings(blockEntity).getComponents().get(i);
            MutableComponent text = getText(component.getStaticText());    
            labels[i].text.set(text);
        }
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].render(graphics);
        }
    }

    private MutableComponent getText(String input) {
        String staticText = VariableManager.replacePlaceholders(input);
        MutableComponent text = TextUtils.empty();
        if (staticText != null) {
            try {
                JsonParser.parseString(staticText);
                text = Component.Serializer.fromJsonLenient(staticText);
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
            this.labels = new BERLabel[0];
            return;
        }
        getDisplaySettings(blockEntity).verifyComponents();
        List<TextComponent> components = getDisplaySettings(blockEntity).getComponents();
        this.labels = new BERLabel[components.size()];

        for (int i = 0; i < components.size(); i++) {
            TextComponent component = components.get(i);
            BERLabel label = new BERLabel();

            MutableComponent text = getText(component.getStaticText());

            label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));
            label.horizontalScrollingSpeed.set(SCROLLING_SPEED);
            label.horizontalScrollMode.set(component.getBoundsAction().getMode());
            label.horizontalMinScale.set(component.getMinXScale());
            label.horizontalMaxScale.set(component.getXScale());
            label.verticalMinScale.set(component.getYScale());
            label.verticalMaxScale.set(component.getYScale());
            label.text.set(text);
            label.color.set(getDisplaySettings(blockEntity).getFontColor());
            label.position.set(Point.of(3 + component.getX(), 3 + component.getY()));
            label.horizontalAlign.set(component.getTextAlignment());
            label.preferredWidth.set((float)(component.getTextMaxWidth() >= StaticTextDisplaySettings.DEFAULT_TEXT_MAX_WIDTH ? Math.min(Float.MAX_VALUE, label.clippingArea.get().width()) : component.getTextMaxWidth()));
            label.backgroundColor.set(component.getTextBackgroundColor());
            label.fullBackground.set(component.isFullLabelBackgroundColor());
            label.glowing.set(blockEntity.isGlowing());

            this.labels[i] = label;
        }
    }
}
