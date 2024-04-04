package de.mrjulsen.crn.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.font.GlyphInfo;

import de.mrjulsen.crn.mixin.BakedGlyphAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;

public class FontUtils {
    public final Font font;
    public final FontSet fontSet;
    
    protected static record UVData(float u0, float v0, float u1, float v1) {}
    protected static final Map<Integer, Deque<UVData>> uvStack = new HashMap<>();

    @SuppressWarnings("resource")
    public FontUtils(ResourceLocation fontStyle) {
        this.font = Minecraft.getInstance().font;
        this.fontSet = this.font.getFontSet(fontStyle);
    }    

    public BakedGlyphAccessor getGlyphAccessor(int charCode) {
        return (BakedGlyphAccessor)getGlyph(charCode);
    }

    public void pushUV(int charCode) {
        BakedGlyphAccessor glyph = getGlyphAccessor(charCode);
        pushUV(charCode, glyph.getU0(), glyph.getV0(), glyph.getU1(), glyph.getV1());
    }

    protected void pushUV(int charCode, float u0, float v0, float u1, float v1) {
        if (!uvStack.containsKey(charCode)) {
            uvStack.put(charCode, new ArrayDeque<>());
        }
        uvStack.get(charCode).addLast(new UVData(u0, v0, u1, v1));
    }

    public boolean popUV(int charCode) {
        if (!uvStack.containsKey(charCode)) {
            return false;
        }

        UVData data = uvStack.get(charCode).pollLast();
        if (uvStack.get(charCode).isEmpty()) {
            uvStack.remove(charCode);
        }
        
        BakedGlyphAccessor glyph = getGlyphAccessor(charCode);
        glyph.setU0(data.u0());
        glyph.setV0(data.v0());
        glyph.setU1(data.u1());
        glyph.setV1(data.v1());
        return true;
    }

    public GlyphInfo getGlyphInfo(int charCode) {
        return fontSet.getGlyphInfo(charCode, false);
    }

    public BakedGlyph getGlyph(int charCode) {
        return fontSet.getGlyph(charCode);
    }

    public void reset() {
        uvStack.clear();
    }
}
