package de.mrjulsen.crn.item.creativemodetab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.item.ModItems;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeModeTab {
    
    private static final Map<ModTab, Collection<RegistryObject<? extends ItemLike>>> CREATIVE_MODE_TAB_REGISTRY = new HashMap<>();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModMain.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = registerTab("createrailwaysnavigatortab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.NAVIGATOR.get()))
            .title(Utils.translate("itemGroup.createrailwaysnavigatortab"))
            .displayItems((pParameters, pOutput) -> {
                Collection<RegistryObject<? extends ItemLike>> content = CREATIVE_MODE_TAB_REGISTRY.get(ModTab.MAIN);
                if (content != null) {
                    pOutput.acceptAll(content.stream().map(x -> new ItemStack(x.get())).toList());
                }
            })
            .build()
        );

    private static RegistryObject<CreativeModeTab> registerTab(String id, Supplier<? extends CreativeModeTab> sup) { 
        RegistryObject<CreativeModeTab> cTab = CREATIVE_MODE_TABS.register(id, sup);
        return cTab;
    }

    public static void register(IEventBus event) {
        CREATIVE_MODE_TABS.register(event);
    }

    public static void put(ModTab tab, RegistryObject<? extends ItemLike> item) {
        if (!CREATIVE_MODE_TAB_REGISTRY.containsKey(tab)) {
            CREATIVE_MODE_TAB_REGISTRY.put(tab, new ArrayList<>());
        }
        CREATIVE_MODE_TAB_REGISTRY.get(tab).add(item);
    }

    public static enum ModTab {
        MAIN;
    }
}
