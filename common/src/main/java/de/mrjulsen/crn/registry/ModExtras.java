package de.mrjulsen.crn.registry;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import com.tterrag.registrate.util.entry.RegistryEntry;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ModExtras {

    private static final DeferredRegister<DisplaySource> DISPLAY_SOURCES = DeferredRegister.create(CreateRailwaysNavigator.MOD_ID, CreateRegistries.DISPLAY_SOURCE);

    private static final RegistrySupplier<AdvancedDisplaySource> ADVANCED_DISPLAY_SOURCE = DISPLAY_SOURCES.register("advanced_display", () -> {
        AdvancedDisplaySource obj = new AdvancedDisplaySource();
        DisplaySource.BY_BLOCK_ENTITY.register(AllBlockEntityTypes.TRACK_STATION.get(), List.of(obj));
        return obj;
    });

    public static final RegistryEntry<AdvancedDisplayTarget> ADVANCED_DISPLAY_BOARD_TARGET = CreateRailwaysNavigator.REGISTRATE.displayTarget("advanced_display", AdvancedDisplayTarget::new)
            .register();

    public static void init() {
        DISPLAY_SOURCES.register();
    }
}
