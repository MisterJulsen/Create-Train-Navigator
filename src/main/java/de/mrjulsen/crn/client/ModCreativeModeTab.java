package de.mrjulsen.crn.client;

import de.mrjulsen.crn.item.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {
    
    public static final CreativeModeTab MOD_TAB = new CreativeModeTab("createrailwaysnavigatortab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.NAVIGATOR.get());
        };
    };

}
