package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.components.NavigatorBackgroundComponent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(CreateRailwaysNavigator.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<NavigatorBackgroundComponent>> NAVIGATOR_BACKGROUND_COMPONENT = COMPONENTS.register("navigator_background_component", () ->
            new DataComponentType.Builder<NavigatorBackgroundComponent>()
                .persistent(NavigatorBackgroundComponent.CODEC)
                .networkSynchronized(NavigatorBackgroundComponent.STREAM_CODEC)
                .build()
    );

    public static void init() {
        COMPONENTS.register();
    }
}
