package de.mrjulsen.crn.client.ber.base;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.mixin.BakedGlyphAccessor;
import de.mrjulsen.crn.util.FontUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringDecomposer;

/**
 * A text component designed for block entity rendering. It supports scissoring and scrolling text, e.g. for display boards.
 */
public class BERText {
    
    private static final byte CHAR_SIZE = 8;

    private final FontUtils fontUtils;
    private final float xOffset;
    private Supplier<List<Component>> textData;

    private float minX = 0;
    private float maxX = Float.MAX_VALUE;
    private float minStretchScale = 1.0f;
    private float maxStretchScale = 1.0f;
    private float maxWidth = Float.MAX_VALUE;
    private boolean forceMaxWidth = false;
    private float scrollSpeed = 0.0f;
    private boolean centered = false;
    private int color = 0xFFFFFFFF;
    private int ticksPerPage = 200;
    private int refreshRate = 0;

    private TextTransformation predefinedTextTransformation = null;

    // stored data
    private TextDataCache cache;
    private float scrollXOffset = 0.0f;
    private int refreshTimer = 0;
    private int currentTicks = 0;
    private int currentIndex = 0;
    private List<Component> texts;

    public BERText(FontUtils fontUtils, Component text, float xOffset) {
        this.fontUtils = fontUtils;
        this.textData = () -> List.of(text);
        this.xOffset = xOffset;
    }

    public BERText(FontUtils fontUtils, Supplier<List<Component>> texts, float xOffset) {
        this.fontUtils = fontUtils;
        this.textData = texts;
        this.xOffset = xOffset;
    }

    public BERText withStencil(float minX, float maxX) {
        this.minX = minX;
        this.maxX = maxX;
        return this;
    }

    public BERText withStretchScale(float minScale, float maxScale) {
        this.minStretchScale = minScale;
        this.maxStretchScale = maxScale;
        return this;
    }

    public BERText withMaxWidth(float maxWidth, boolean force) {
        this.maxWidth = maxWidth;
        this.forceMaxWidth = force;
        return this;
    }

    public BERText withIsCentered(boolean b) {
        this.centered = b;
        return this;
    }

    public BERText withCanScroll(boolean b, float scrollingSpeed) {
        this.scrollSpeed = b ? scrollingSpeed : 0;
        return this;
    }

    public BERText withColor(int color) {
        this.color = color;
        return this;
    }

    public BERText withTicksPerPage(int ticks) {
        this.ticksPerPage = ticks;
        return this;
    }

    /**
     * The displayed text is updated every x ticks. If the value is less than or equal to 0, then the text will not be updated.
     */
    public BERText withRefreshRate(int ticks) {
        this.refreshRate = ticks;
        return this;
    }

    public BERText withPredefinedTextTransformation(TextTransformation transformation) {
        this.predefinedTextTransformation = transformation;
        return this;
    }

    public BERText build() {
        fetchCurrentText();
        calc();
        scrollXOffset = cache.maxWidthScaled();
        return this;
    }

    public FontUtils getFontUtils() {
        return fontUtils;
    }

    private void fetchCurrentText() {
        texts = textData.get();
    } 
    
    public Component getCurrentText() {
        return texts.get(currentIndex);
    }    

    public List<Component> getTexts() {
        return texts;
    }

    public int getTicksPerPage() {
        return ticksPerPage;
    }

    public float getXOffset() {
        return xOffset;
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinStretchScale() {
        return minStretchScale;
    }

    public float getMaxStretchScale() {
        return maxStretchScale;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    public boolean forceMaxWidth() {
        return forceMaxWidth;
    }

    public boolean canScroll() {
        return scrollSpeed > 0;
    }

    public float getScrollSpeed() {
        return scrollSpeed;
    }

    public boolean isCentered() {
        return centered;
    }

    public float getTextWidth() {
        return getFontUtils().font.width(getCurrentText());
    }

    public float getScaledTextWidth() {
        return getTextWidth() * cache.textXScale();
    }

    public int getColor() {
        return color;
    }

    public void calc() {
        float textWidth = getFontUtils().font.width(getCurrentText());
        float rawXScale = getMaxWidth() / textWidth;
        float finalXScale = de.mrjulsen.mcdragonlib.utils.Math.clamp(rawXScale, getMinStretchScale(), getMaxStretchScale());
        boolean mustScroll = rawXScale < getMinStretchScale();

        if (forceMaxWidth() && mustScroll) {
            finalXScale = getMaxStretchScale();
        }

        float minX = getMinX() / finalXScale;
        float maxWidthScaled = getMaxWidth() / finalXScale;
        float maxX = Math.min(forceMaxWidth() ? maxWidthScaled : Float.MAX_VALUE, getMaxX() / finalXScale);
        float xOffset = getXOffset() + (isCentered() ? maxWidthScaled / 2 - textWidth / 2 : 0);
        cache = new TextDataCache(finalXScale, minX, maxX, xOffset, maxWidthScaled, textWidth, forceMaxWidth() && mustScroll);
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
        getFontUtils().reset();
        pPoseStack.pushPose(); {
            if (predefinedTextTransformation != null) {
                pPoseStack.translate(predefinedTextTransformation.x(), predefinedTextTransformation.y(), predefinedTextTransformation.z());
                pPoseStack.scale(predefinedTextTransformation.xScale(), predefinedTextTransformation.yScale(), 1);
            }

            pPoseStack.pushPose(); {                
                pPoseStack.scale(cache.textXScale(), 1, 1);
                renderTextInBounds(pPoseStack, getFontUtils(), pBufferSource, getCurrentText(), pPackedLight, cache.mustScroll ? scrollXOffset : cache.xOffset(), cache.minX(), cache.maxX(), getColor());
            }
            pPoseStack.popPose();
        }
        pPoseStack.popPose();
    }

    private void renderTextInBounds(PoseStack pPoseStack, FontUtils fontUtils, MultiBufferSource pBufferSource, Component text, int pPackedLight, float xOffset, float xLeft, float xRight, int color) {
        if (xRight <= xLeft) {
            return;
        }

        pPoseStack.pushPose();
        pPoseStack.translate(xLeft + (xOffset > 0 ? xOffset : 0), 0, 0);
        Font.StringRenderOutput sro = fontUtils.font.new StringRenderOutput(pBufferSource, 0, 0, color, false, pPoseStack.last().pose(), Font.DisplayMode.NORMAL, pPackedLight);
        
        float newX = xOffset;
        float glyphTranslation = 0;
        final float charSize = CHAR_SIZE + (text.getStyle().isBold() ? 1 : 0);

        for (int i = 0; i < text.getString().length(); i++) {
            int charCode = text.getString().charAt(i);
            GlyphInfo info = fontUtils.fontSet.getGlyphInfo(charCode);
            float glyphWidth = info.getAdvance(text.getStyle().isBold());
            float oldX = newX;
            newX += glyphWidth;

            if (newX > xLeft && oldX < xLeft) {
                float diff = xLeft - oldX;
                BakedGlyphAccessor glyph = fontUtils.getGlyphAccessor(charCode);                
                float glyphUVDiff = glyph.getU1() - glyph.getU0();
                float scale = (1.0f / charSize * diff);
                float sub = glyphUVDiff * scale;

                fontUtils.pushUV(charCode);
                glyph.setU0(glyph.getU0() + sub);

                pPoseStack.pushPose();
                float invScale = 1.0f - scale;
                pPoseStack.scale(invScale, 1, 1);
                Font.StringRenderOutput sro2 = fontUtils.font.new StringRenderOutput(pBufferSource, 0 , 0, color, false, pPoseStack.last().pose(), Font.DisplayMode.NORMAL, pPackedLight);
                StringDecomposer.iterateFormatted(String.valueOf((char)charCode), text.getStyle(), sro2);
                pPoseStack.popPose();
                fontUtils.popUV(charCode);
                pPoseStack.translate(glyphWidth - (charSize * scale), 0, 0);
                continue;
            } else if (newX > xRight) {
                float diff = newX - xRight;
                float charRightSpace = charSize - glyphWidth;
                float totalDiff = diff + charRightSpace;

                BakedGlyphAccessor glyph = fontUtils.getGlyphAccessor(charCode);
                float glyphUVDiff = glyph.getU1() - glyph.getU0();
                float scale = (1.0f / charSize * totalDiff);
                float sub = glyphUVDiff * scale;

                fontUtils.pushUV(charCode);
                glyph.setU1(glyph.getU1() - sub);
                pPoseStack.pushPose();
                float invScale = 1.0f - scale;
                pPoseStack.scale(invScale, 1, 1);
                pPoseStack.translate(glyphTranslation / invScale, 0, 0);
                Font.StringRenderOutput sro2 = fontUtils.font.new StringRenderOutput(pBufferSource, 0, 0, color, false, pPoseStack.last().pose(), Font.DisplayMode.NORMAL, pPackedLight);
                StringDecomposer.iterateFormatted(String.valueOf((char)charCode), text.getStyle(), sro2);
                pPoseStack.popPose();
                fontUtils.popUV(charCode);
                break;
            } else if (oldX >= xLeft && newX <= xRight) {
                StringDecomposer.iterateFormatted(String.valueOf((char)charCode), text.getStyle(), sro);
            } else {
                continue;
            }

            glyphTranslation += glyphWidth;
        }
        pPoseStack.popPose();
    }

    public void tick() {
        
        if (refreshRate > 0) {
            refreshTimer++;
            if ((refreshTimer %= refreshRate) == 0) {
                fetchCurrentText();
                calc();
            }
        }

        boolean multiText = getTexts().size() > 1;

        if (cache.mustScroll()) {
            scrollXOffset -= getScrollSpeed() / this.getMaxStretchScale();
            if (scrollXOffset < -cache.textWidth()) {
                scrollXOffset = cache.maxWidthScaled();

                if (multiText) {
                    currentIndex++;
                    fetchCurrentText();
                    currentIndex %= getTexts().size();
                    calc();
                }
            }
        } else if (multiText) {
            currentTicks++;
            if ((currentTicks %= getTicksPerPage()) == 0) {
                currentIndex++;
                fetchCurrentText();
                currentIndex %= getTexts().size();
                calc();
            }
        }
    }

    protected static record TextDataCache(float textXScale, float minX, float maxX, float xOffset, float maxWidthScaled, float textWidth, boolean mustScroll) {}
    public static record TextTransformation(float x, float y, float z, float xScale, float yScale) {}

}
