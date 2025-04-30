package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class DLNewListBox<T, W extends DLNewListBox.Entry<T, W>> extends DLScrollableWidgetContainer {

    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;
    private final Map<W, T> values = new HashMap<>();
    private final List<W> entryWidgets = new ArrayList<>();
    private List<T> currentData = null;
    private EntryBuilderContext<T, W> itemBuilderContext;

    private W draggedEntry;
    private double relMousePosY;
    private int dropIndex = 0;
    private int markerYPos;

    public DLNewListBox(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.setMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public Screen getParent() {
        return parent;
    }

    public void displayData(List<T> data, EntryBuilderContext<T, W> createItem) {
        clearWidgets();
        values.clear();
        entryWidgets.clear();
        this.currentData = data;
        this.itemBuilderContext = createItem;
        contentHeight = 0;
        for (int i = 0; i < data.size(); i++) {
            T entry = data.get(i);
            W widget = createItem.build(this, i, entry);
            if (widget == null) continue;
            widget.set_x(x());
            widget.set_width(width());
            widget.set_y(y() + contentHeight);        
            entryWidgets.add(addRenderableWidget(widget));
            values.put(widget, entry);
            contentHeight += widget.height();
        }
        scrollBar.setMaxScroll(contentHeight);
    }

    public Set<Map.Entry<W, T>> getEntries() {
        return values.entrySet();
    }

    public void updateDropIndex(double mouseY) {
        if (draggedEntry == null) return;
        relMousePosY = (int)(mouseY - y() + scrollBar.getScrollValue());
        int cumulative = 0;
        int nonDraggedIndex = 0;
        for (W widget : entryWidgets) {
            if (widget == draggedEntry) continue;
            int widgetHeight = widget.height();
            int widgetBottom = cumulative + widgetHeight;
            if (relMousePosY < widgetBottom) {
                dropIndex = nonDraggedIndex;
                return;
            }
            cumulative = widgetBottom;
            nonDraggedIndex++;
        }
        dropIndex = nonDraggedIndex;
    }
    
    public void layoutEntries() {
        int currentY = y();
        int nonDraggedIndex = 0;
        for (W widget : entryWidgets) {
            if (widget == draggedEntry) continue;
            if (nonDraggedIndex == dropIndex && draggedEntry != null) {
                markerYPos = currentY;
                currentY += draggedEntry.height();
            }
            widget.set_y(currentY);
            currentY += widget.height();
            nonDraggedIndex++;
        }        
        if (nonDraggedIndex == dropIndex && draggedEntry != null) {
            markerYPos = currentY;
            currentY += draggedEntry.height();
        }
        contentHeight = currentY - y();
    }

    public void finalizeDrag() {
        if (draggedEntry != null && currentData != null) {
            int oldIndex = entryWidgets.indexOf(draggedEntry);
            int newIndex = dropIndex >= 0 ? dropIndex : oldIndex;
            if (oldIndex >= 0 && newIndex >= 0 && oldIndex < entryWidgets.size() && newIndex < entryWidgets.size()) {                
                T element = currentData.remove(oldIndex);
                currentData.add(newIndex, element);
            }
            draggedEntry.resetDrag();
            draggedEntry = null;
            dropIndex = -1;
            displayData(currentData, itemBuilderContext);
        }
    }

    protected void autoScroll() {
        final int margin = 16;
        final double speedFac = 0.25;
        double mousePos = relMousePosY - scrollBar.getScrollValue();

        if (mousePos < margin) {
            scrollBar.setScrollValue(scrollBar.getScrollValue() - speedFac * -(mousePos - margin));
        } else if (mousePos > height() - margin) {
            scrollBar.setScrollValue(scrollBar.getScrollValue() + speedFac * (mousePos - (height() - margin)));
        }
    }

    public void setDraggedEntry(W entry) {
        if (draggedEntry == null) {
            draggedEntry = entry;
        }
    }
    
    @Override
    public void tick() {
        if (draggedEntry != null && !parent.isDragging()) {
            finalizeDrag();
        }
        if (draggedEntry != null) {
            autoScroll();
            layoutEntries();
        }
        super.tick();
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (draggedEntry != null) {
            updateDropIndex(mouseY);
            layoutEntries();
        }
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        
        if (scrollBar.getScrollValue() > 0) {
            GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        }
        if (scrollBar.getScrollValue() < scrollBar.getMaxScroll()) {
            GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
        }

        GuiUtils.enableScissor(graphics, x(), y(), width(), height());
        if (draggedEntry != null) {
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(0, -scrollBar.getScrollValue(), 0);
            GuiUtils.drawBox(graphics, new GuiAreaDefinition(x(), markerYPos, width(), draggedEntry.height()), getPulsatingColor((double)Minecraft.getInstance().level.getGameTime(), 0.1, 0x44FFFFFF, (byte)0x00, (byte)0x55), 0xFFFFFFFF);
            GuiUtils.drawString(graphics, font, x(), markerYPos, String.valueOf(dropIndex), 0xFFFFFFFF, EAlignment.LEFT, false);
            graphics.poseStack().popPose();
        }
        GuiUtils.disableScissor(graphics);;
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {        
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);

        if (draggedEntry != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(-(x()) + (mouseX - draggedEntry.grabOffsetX), -(y() + (draggedEntry.y() - y())) + (int)(mouseY - draggedEntry.grabOffsetY), 100);
            CreateDynamicWidgets.renderShadow(graphics, -2 + draggedEntry.x(), -2 + draggedEntry.y(), draggedEntry.width() + 4, draggedEntry.height() + 4);
            CreateDynamicWidgets.renderSingleShadeWidget(graphics, -2 + draggedEntry.x(), -2 + draggedEntry.y(), draggedEntry.width() + 4, draggedEntry.height() + 4, ColorShade.DARK);
            draggedEntry.renderItem(graphics, mouseX, mouseY, partialTicks);
            graphics.poseStack().popPose();
        }
        
    }

    public interface EntryBuilderContext<S, T extends Entry<S, T>> {
        T build(DLNewListBox<S, T> listBox, int index, S data);
    }

        public static abstract class Entry<S, T extends Entry<S, T>> extends DLWidgetContainer {

        private final DLNewListBox<S, T> list;
        private final S data;
        private int index;

        private double initialMouseX;
        private double initialMouseY;
        private double dragOffsetX;
        private double dragOffsetY;
        private boolean isDragging = false;
        int grabOffsetX;
        int grabOffsetY;
    
        protected Entry(DLNewListBox<S, T> list, int index, S data, int height) {
            super(0, 0, 0, height);
            this.list = list;
            this.data = data;
            this.index = index;
        }

        public final S getData() {
            return data;
        }

        public final int getIndex() {
            return index;
        }

        public final DLNewListBox<S, T> getList() {
            return list;
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.initialMouseY = mouseY;
            this.initialMouseX = mouseX;
            this.grabOffsetX = (int)(mouseX - x());
            this.grabOffsetY = (int)(mouseY - y());
            return super.mouseClicked(mouseX, mouseY, button);
        }


        @SuppressWarnings("unchecked")
        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            
            if (!isDragging && (Math.abs(dragOffsetX) > 5 || Math.abs(dragOffsetY) > 5)) {
                isDragging = true;
                list.setDraggedEntry((T)this);
            } else if (!isDragging) {
                dragOffsetX = mouseX - initialMouseX;
                dragOffsetY = mouseY - initialMouseY;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            isDragging = false;
            resetDrag();
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public final void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (isDragging) {
                return;
            }
            renderItem(graphics, mouseX, mouseY, partialTicks);
        }
        
        public void renderItem(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        }
        
        public void resetDrag() {
            isDragging = false;
            dragOffsetX = 0;
            dragOffsetY = 0;
        }

        @Override
        public void tick() {
            if (!list.parent.isDragging()) {
                resetDrag();
            }
            super.tick();
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }
    
        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
        }
    
        @Override
        public boolean consumeScrolling(double mouseX, double mouseY) {
            return false;
        }
    }


    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }



    public static int getPulsatingColor(double time, double speed, int color, byte alphaMin, byte alphaMax) {
        int aMin = alphaMin & 0xFF;
        int aMax = alphaMax & 0xFF;

        if (aMin > aMax) {
            int temp = aMin;
            aMin = aMax;
            aMax = temp;
        }

        int alphaRange = aMax - aMin;
        double sineValue = Math.sin(time * speed);
        double normalized = (sineValue + 1) / 2.0;
        int alpha = aMin + (int)(normalized * alphaRange);
        return (alpha << 24) | (color & 0xFFFFFF);
    }
    
}