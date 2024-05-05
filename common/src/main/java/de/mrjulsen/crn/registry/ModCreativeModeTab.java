package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(CreateRailwaysNavigator.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
        "createrailwaysnavigatortab", // Tab ID
        () -> CreativeTabRegistry.create(
                TextUtils.translate("itemGroup.createrailwaysnavigator.tab"), // Tab Name
                () -> new ItemStack(ModItems.NAVIGATOR.get()) // Icon
        )
    );

    public static void setup() {
        CREATIVE_MODE_TABS.register();
    }
}