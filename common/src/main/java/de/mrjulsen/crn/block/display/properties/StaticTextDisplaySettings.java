package de.mrjulsen.crn.block.display.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.mrjulsen.crn.block.display.properties.components.GuiBuilderWrapper;
import de.mrjulsen.crn.block.display.properties.components.IStaticTextSetting;
import de.mrjulsen.crn.block.display.properties.components.ITextBackgroundColorSetting;
import de.mrjulsen.crn.block.display.properties.components.ITextPosSetting;
import de.mrjulsen.crn.block.display.properties.components.ITextScaleSetting;
import de.mrjulsen.crn.block.display.properties.components.ITextWidthSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class StaticTextDisplaySettings extends BasicDisplaySettings implements IStaticTextSetting, ITextScaleSetting, ITextPosSetting, ITextWidthSetting, ITextBackgroundColorSetting {

    public static class TextComponent {
        String staticText = "";
        float xScale = DEFAULT_SCALE;
        float yScale = DEFAULT_SCALE;
        float minXScale = DEFAULT_SCALE;
        TextScaleBounds bounds = DEFAULT_BOUNDS_ACTION;
        float x = DEFAULT_X;
        float y = DEFAULT_Y;
        float maxWidth = DEFAULT_TEXT_MAX_WIDTH;
        ETextAlignment alignment = DEFAULT_TEXT_ALIGNMENT;
        DLColor backgroundColor = DEFAULT_BG_COLOR;
        boolean fullLabelColor = DEFAULT_FULL_LABEL_COLOR;

        public TextComponent() {

        }

        public TextComponent(String text) {
            this.staticText = text;
        }
    
        public String getStaticText() {
            return this.staticText;
        }

        public void setStaticText(String text) {
            this.staticText = text == null ? "" : text;
        }
        
        public float getXScale() {
            return this.xScale;
        }

        public void setXScale(float xScale) {
            this.xScale = xScale;
        }

        public float getYScale() {
            return this.yScale;
        }

        public void setYScale(float f) {
            this.yScale = f;
        }

        public float getMinXScale() {
            return this.minXScale;
        }

        public void setMinXScale(float f) {
            this.minXScale = f;
        }

        public TextScaleBounds getBoundsAction() {
            return this.bounds;
        }

        public void setBoundsAction(TextScaleBounds action) {
            this.bounds = action;
        }

        public float getX() {
            return this.x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return this.y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public ETextAlignment getTextAlignment() {
            return this.alignment;
        }

        public void setTextAlignment(ETextAlignment alignment) {
            this.alignment = alignment;
        }

        public float getTextMaxWidth() {
            return this.maxWidth;
        }

        public void setTextMaxWidth(float s) {
            this.maxWidth = s;
        }

        public DLColor getTextBackgroundColor() {
            return this.backgroundColor;
        }

        public void setTextBackgroundColor(DLColor color) {
            this.backgroundColor = color;
        }

        public boolean isFullLabelBackgroundColor() {
            return this.fullLabelColor;
        }

        public void setFullLabelBackgroundColor(boolean b) {
            this.fullLabelColor = b;
        }

        public static TextComponent fromNbt(CompoundTag nbt) {
            TextComponent comp = new TextComponent();
            if (nbt.contains(NBT_TEXT)) comp.staticText = nbt.getString(NBT_TEXT);
            if (nbt.contains(NBT_X_SCALE)) comp.xScale = nbt.getFloat(NBT_X_SCALE);
            if (nbt.contains(NBT_Y_SCALE)) comp.yScale = nbt.getFloat(NBT_Y_SCALE);
            if (nbt.contains(NBT_X_MIN_SCALE)) comp.minXScale = nbt.getFloat(NBT_X_MIN_SCALE);
            if (nbt.contains(NBT_BOUNDS_ACTION)) comp.bounds = TextScaleBounds.getByIndex(nbt.getByte(NBT_BOUNDS_ACTION));
            if (nbt.contains(NBT_POS_X)) comp.x = nbt.getFloat(NBT_POS_X);
            if (nbt.contains(NBT_POS_Y)) comp.y = nbt.getFloat(NBT_POS_Y);
            if (nbt.contains(NBT_TEXT_MAX_WIDTH)) comp.maxWidth = nbt.getFloat(NBT_TEXT_MAX_WIDTH);
            if (nbt.contains(NBT_TEXT_ALIGNMENT)) comp.alignment = ETextAlignment.getById(nbt.getInt(NBT_TEXT_ALIGNMENT));
            if (nbt.contains(NBT_TEXT_BG_COLOR)) comp.backgroundColor = DLColor.fromInt(nbt.getInt(NBT_TEXT_BG_COLOR));
            if (nbt.contains(NBT_FULL_LABEL_COLOR)) comp.fullLabelColor = nbt.getBoolean(NBT_FULL_LABEL_COLOR);
            return comp;
        }
    
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_TEXT, staticText);
            nbt.putFloat(NBT_X_SCALE, xScale);
            nbt.putFloat(NBT_Y_SCALE, yScale);
            nbt.putFloat(NBT_X_MIN_SCALE, minXScale);
            nbt.putByte(NBT_BOUNDS_ACTION, bounds.getIndex());
            nbt.putFloat(NBT_POS_X, x);
            nbt.putFloat(NBT_POS_Y, y);
            nbt.putFloat(NBT_TEXT_MAX_WIDTH, maxWidth);
            nbt.putInt(NBT_TEXT_ALIGNMENT, alignment.getId());
            nbt.putInt(NBT_TEXT_BG_COLOR, backgroundColor.getAsARGB());
            nbt.putBoolean(NBT_FULL_LABEL_COLOR, fullLabelColor);
            return nbt;
        }

        @Override
        public String toString() {
            return staticText;
        }
    }

    public static final byte MAX_COMPONENTS = 50;
    private static final String NBT_COMPONENTS = "Components";
    private static final String NBT_SELECTED_COMPONENT = "SelectedComponent";
    private final List<TextComponent> components = new ArrayList<>();
    protected byte selectedComponent = 0;

    public StaticTextDisplaySettings() {
        components.add(new TextComponent(DEFAULT_TEXT));
    }
    

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_COMPONENTS)) {
            this.components.clear();
            this.components.addAll(nbt.getList(NBT_COMPONENTS, Tag.TAG_COMPOUND).stream().map(x -> TextComponent.fromNbt((CompoundTag)x)).toList());
        }
        if (nbt.contains(NBT_SELECTED_COMPONENT)) this.selectedComponent = nbt.getByte(NBT_SELECTED_COMPONENT);

        if (components.isEmpty()) {
            components.add(new TextComponent(DEFAULT_TEXT));
        }
        verifyComponents();
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        verifyComponents();
        super.serializeNbt(nbt);
        ListTag list = new ListTag();
        int k = 0;
        for (TextComponent component : components) {
            list.add(component.toNbt());
            k++;
            if (k >= MAX_COMPONENTS) {
                break;
            }
        }
        nbt.put(NBT_COMPONENTS, list);
        nbt.putByte(NBT_SELECTED_COMPONENT, selectedComponent);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        GuiBuilderWrapper.buildStaticTextBaseGui(this, context);
        this.buildStaticTextGui(context);
        this.buildTextPosGui(context);
        this.buildTextScaleGui(context);
        this.buildTextMaxWidthGui(context);
        this.buildTextBackgroundColorGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        int currentIndex = getSelectedComponentIndex();
        if (oldSettings instanceof StaticTextDisplaySettings o && !o.components.isEmpty() && this != o) {
            components.clear();
            for (byte i = 0; i < o.getComponentsCount(); i++) {
                components.add(new TextComponent());
                this.selectedComponent = i;
                o.selectedComponent = i;
                copyStaticTextSettings(oldSettings);
                copyTextScaleSettings(oldSettings);
                copyTextPosSettings(oldSettings);
                copyTextMaxWidthSetting(oldSettings);
                copyTextBackgroundColorSettings(oldSettings);
            }
        } else { 
               
            copyStaticTextSettings(oldSettings);
            copyTextScaleSettings(oldSettings);
            copyTextPosSettings(oldSettings);
            copyTextMaxWidthSetting(oldSettings);
            copyTextBackgroundColorSettings(oldSettings);
        }
        setSelectedComponentIndex(currentIndex);
    }

    public TextComponent getSelectedComponent() {
        return components.isEmpty() ? new TextComponent() : components.get(MathUtils.clamp(selectedComponent, 0, components.size() - 1));
    }

    public List<TextComponent> getComponents() {
        return ImmutableList.copyOf(this.components);
    }

    public void addComponent(TextComponent component) {
        components.add(component);
        verifyComponents();
    }

    public void setComponent(int index, TextComponent component) {
        components.set(index, component);
    }

    public int getComponentsCount() {
        return components.size();
    }

    public byte getSelectedComponentIndex() {
        return selectedComponent;
    }

    public void setSelectedComponentIndex(int index) {
        this.selectedComponent = (byte)MathUtils.clamp(index, 0, components.size() - 1);
    }



    
    @Override
    public String getStaticText() {
        return getSelectedComponent().staticText;
    }

    @Override
    public void setStaticText(String text) {
        getSelectedComponent().staticText = text;
    }
    
    @Override
    public float getXScale() {
        return getSelectedComponent().xScale;
    }

    @Override
    public void setXScale(float xScale) {
        getSelectedComponent().xScale = xScale;
    }

    @Override
    public float getYScale() {
        return getSelectedComponent().yScale;
    }

    @Override
    public void setYScale(float f) {
        getSelectedComponent().yScale = f;
    }

    @Override
    public float getMinXScale() {
        return getSelectedComponent().minXScale;
    }

    @Override
    public void setMinXScale(float f) {
        getSelectedComponent().minXScale = f;
    }

    @Override
    public TextScaleBounds getBoundsAction() {
        return getSelectedComponent().bounds;
    }

    @Override
    public void setBoundsAction(TextScaleBounds action) {
        getSelectedComponent().bounds = action;
    }

    @Override
    public float getX() {
        return getSelectedComponent().x;
    }

    @Override
    public void setX(float x) {
        getSelectedComponent().x = x;
    }

    @Override
    public float getY() {
        return getSelectedComponent().y;
    }

    @Override
    public void setY(float y) {
        getSelectedComponent().y = y;
    }

    @Override
    public ETextAlignment getTextAlignment() {
        return getSelectedComponent().alignment;
    }

    @Override
    public void setTextAlignment(ETextAlignment alignment) {
        getSelectedComponent().alignment = alignment;
    }

    @Override
    public float getTextMaxWidth() {
        return getSelectedComponent().maxWidth;
    }

    @Override
    public void setTextMaxWidth(float s) {
        getSelectedComponent().maxWidth = s;
    }

    @Override
    public DLColor getTextBackgroundColor() {
        return getSelectedComponent().backgroundColor;
    }

    @Override
    public void setTextBackgroundColor(DLColor color) {
        getSelectedComponent().backgroundColor = color;
    }

    @Override
    public boolean isFullLabelBackgroundColor() {
        return getSelectedComponent().fullLabelColor;
    }

    @Override
    public void setFullLabelBackgroundColor(boolean b) {
        getSelectedComponent().fullLabelColor = b;
    }

    public void verifyComponents() {
        TextComponent current = getSelectedComponentIndex() > 0 && getSelectedComponentIndex() < this.components.size() ? this.components.get(getSelectedComponentIndex()) : null;
        Iterator<TextComponent> components = this.components.iterator();
        int i = 1;
        while (components.hasNext()) {
            TextComponent comp = components.next();
            if (i > MAX_COMPONENTS || comp.staticText == null || comp.staticText.isBlank()) {
                components.remove();
            }
            i++;
        }
        int k = current == null ? -1 : this.components.indexOf(current);
        if (k < 0) {
            this.setSelectedComponentIndex(getSelectedComponentIndex());
        }
    }

    public void createNewComponent() {
        if (components.size() < MAX_COMPONENTS) {
            this.components.add(new TextComponent());
        }
    }
}
