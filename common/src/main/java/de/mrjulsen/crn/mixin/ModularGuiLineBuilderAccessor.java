package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.minecraft.client.gui.Font;

@Mixin(ModularGuiLineBuilder.class)
public interface ModularGuiLineBuilderAccessor {
    @Accessor("target")
    ModularGuiLine crn$getTarget();
    @Accessor("font")
	Font crn$getFont();
    @Accessor("x")
	int crn$getX();
    @Accessor("y")
	int crn$getY();
}
