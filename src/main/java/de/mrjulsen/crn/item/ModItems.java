package de.mrjulsen.crn.item;

import de.mrjulsen.crn.ModMain;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModMain.MOD_ID);

    
    public static final RegistryObject<Item> NAVIGATOR = ITEMS.register("navigator", () -> new NavigatorItem());
    
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);        
    }
}
