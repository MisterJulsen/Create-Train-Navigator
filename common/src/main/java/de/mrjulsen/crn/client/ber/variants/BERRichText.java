package de.mrjulsen.crn.client.ber.variants;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings.TextComponent;
import de.mrjulsen.crn.block.display.properties.components.ITextWidthSetting.TextScaleBounds;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.util.VariableManager;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERRichText implements AbstractAdvancedDisplayRenderer<StaticTextDisplaySettings> {


    private BERLabel[] labels = new BERLabel[0];

    @Override
    public void renderTick(float deltaTime) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].renderTick();
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        for (int i = 0; i < labels.length && i < getDisplaySettings(blockEntity).getComponents().size(); i++) {
            TextComponent component = getDisplaySettings(blockEntity).getComponents().get(i);
            MutableComponent text = getText(component.getStaticText());    
            labels[i].setText(text);
        }
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].render(graphics, light);
        }
    }

    private MutableComponent getText(String input) {
        String staticText = VariableManager.replacePlaceholders(input);
        MutableComponent text = TextUtils.empty();
        if (staticText != null) {
            try {
                JsonElement elem = JsonParser.parseString(staticText);
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
    
            label
                .setScrollingSpeed(2)
                .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
                .setText(text)
                .setYScale(component.getYScale())
                .setForceScrolling(component.getBoundsAction() == TextScaleBounds.SCROLL)
                .setScale(component.getXScale(), component.getMinXScale())
                .setPos(3 + component.getX(), 3 + component.getY())
                .setCentered(component.getTextAlignment() == EAlignment.CENTER)
            ;
            label
                .setMaxWidth(Math.min(blockEntity.getXSizeScaled() * 16 - 3 - label.getX(), component.getTextMaxWidth() > StaticTextDisplaySettings.DEFAULT_TEXT_MAX_WIDTH ? Float.MAX_VALUE : component.getTextMaxWidth()), component.getBoundsAction().hit())
            ;
            if (component.getTextAlignment() == EAlignment.RIGHT && !label.isForceScrolling()) {
                label
                    .setPos(label.getX() + label.getMaxWidth() - Math.min(label.getTextWidth(), label.getMaxWidth()), label.getY())
                ;
            }
            label
                .setPos(label.getX(), Math.min(label.getY(), blockEntity.getYSize() * 16 - 2 - label.getYScale() * Minecraft.getInstance().font.lineHeight))
                .setBackground(component.getTextBackgroundColor(), label.isForceScrolling() || (component.isFullLabelBackgroundColor() && component.getTextAlignment() != EAlignment.RIGHT))
            ;

            this.labels[i] = label;
        }
    }
}
