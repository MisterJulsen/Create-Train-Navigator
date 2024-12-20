package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class DLLabel extends DLRenderable {

    protected final Font font = Minecraft.getInstance().font;
    protected boolean autoWidth;
    protected boolean autoHeight;
    private Component text = TextUtils.empty();
    private int tint = 0xFFFFFFFF;
    private EAlignment alignment = EAlignment.CENTER;
    private boolean drawShadow = true;
    

    public DLLabel(int x, int y, int width, int height, Component text) {
        super(x, y, width, height);
        this.text = text;
    }

    public DLLabel setAutoSize(boolean b) {
        this.autoWidth = b;
        this.autoHeight = b;
        return this;
    }
    
    public DLLabel setAutoWidth(boolean b) {
        this.autoWidth = b;
        return this;
    }

    public DLLabel setAutoHeight(boolean b) {
        this.autoHeight = b;
        return this;
    }

    public DLLabel setText(Component text) {
        this.text = text;
        updateSize();
        return this;
    }

    public DLLabel setTint(int tint) {
        this.tint = tint;
        updateSize();
        return this;
    }

    public DLLabel setAlignment(EAlignment alignment) {
        this.alignment = alignment;
        updateSize();
        return this;
    }

    public DLLabel setDrawShadow(boolean b) {
        this.drawShadow = b;
        return this;
    }

    public boolean isAutoWidth() {
        return autoWidth;
    }

    public boolean isAutoHeight() {
        return autoHeight;
    }

    public boolean isAutoSize() {
        return isAutoWidth() || isAutoHeight();
    }

    public Component getText() {
        return text;
    }

    public int getTint() {
        return tint;
    }

    public EAlignment getAlignment() {
        return alignment;
    }

    public boolean isDrawShadow() {
        return drawShadow;
    }

    protected void updateSize() {
        if (this.autoWidth) this.set_width(font.width(text));
        if (this.autoHeight) this.set_height(font.lineHeight);
    }

    private final Cache<Component> textCache = new Cache<>(() -> {
        final boolean tooWide = font.width(getText()) > width();
        return tooWide ? TextUtils.text(font.substrByWidth(getText(), width()).getString() + "...") : getText();    
    });

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        EAlignment align = getAlignment();
        int x = x() + switch (align) {
            case RIGHT -> width();
            case CENTER -> width() / 2;
            default -> 0;
        };
        GuiUtils.drawString(graphics, font, x, y() + height() / 2 - font.lineHeight / 2, textCache.get(), getTint(), getAlignment(), isDrawShadow());
    }
}
