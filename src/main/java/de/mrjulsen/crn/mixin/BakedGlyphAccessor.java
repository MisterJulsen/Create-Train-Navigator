package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

@Mixin(BakedGlyph.class)
public interface BakedGlyphAccessor {
    
	@Mutable
    @Accessor("u0")
    void setU0(float value);
	
    @Accessor("u0")
    float getU0();

	@Mutable
    @Accessor("u1")
    void setU1(float value);

    @Accessor("u1")
    float getU1();


    @Mutable
    @Accessor("v0")
    void setV0(float value);

    @Accessor("v0")
    float getV0();

    @Mutable
    @Accessor("v1")
    void setV1(float value);
	
    @Accessor("v1")
    float getV1();

}
