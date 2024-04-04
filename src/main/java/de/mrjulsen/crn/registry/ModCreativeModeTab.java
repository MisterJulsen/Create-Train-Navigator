package de.mrjulsen.crn.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab extends CreativeModeTab {
    public static ModCreativeModeTab MAIN;
    
    public ModCreativeModeTab(String name) {
        super(name);
        MAIN = this;
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(ModItems.NAVIGATOR.get());
    }
}