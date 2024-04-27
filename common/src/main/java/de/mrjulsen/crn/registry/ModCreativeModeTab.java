package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.ExampleMod;
import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {
    public static final CreativeModeTab MAIN = CreativeTabRegistry.create(
        new ResourceLocation(ExampleMod.MOD_ID, "tab"),
        () -> new ItemStack(ModItems.NAVIGATOR.get())
    );

    public static void setup() {}
}