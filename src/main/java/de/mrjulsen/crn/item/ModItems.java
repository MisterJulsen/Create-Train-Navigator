package de.mrjulsen.crn.item;

import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.item.creativemodetab.ModCreativeModeTab;
import de.mrjulsen.crn.item.creativemodetab.ModCreativeModeTab.ModTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModMain.MOD_ID);

    
    public static final RegistryObject<Item> NAVIGATOR = registerItem("navigator", () -> new NavigatorItem(), ModTab.MAIN);
    

    private static RegistryObject<Item> registerItem(String id, Supplier<? extends Item> sup, ModTab tab) { 
        RegistryObject<Item> item = ITEMS.register(id, sup);
        ModCreativeModeTab.put(tab, item);
        return item;
    }
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);        
    }
}

