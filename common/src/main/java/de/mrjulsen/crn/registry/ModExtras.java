package de.mrjulsen.crn.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;
import de.mrjulsen.crn.mixin.CreateAccessor;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModExtras {
    public static final RegistryEntry<AdvancedDisplaySource> ADVANCED_DISPLAY_SOURCE = CreateRailwaysNavigator.REGISTRATE.displaySource("advanced_display", AdvancedDisplaySource::new).register();
            
    public static final RegistryEntry<AdvancedDisplayTarget> ADVANCED_DISPLAY_BOARD_TARGET = CreateRailwaysNavigator.REGISTRATE.displayTarget("advanced_display", AdvancedDisplayTarget::new)
        .register();

    private static void checkAndAssignSource(Pair<Block, DisplaySource> pair) {
        if (pair.getFirst() == null || pair.getSecond() == null) return;
        DisplaySource.BY_BLOCK.add(pair.getFirst(), pair.getSecond());
    }

    private static void assignSource(AbstractRegistrate<?> blockReg, ResourceLocation blockId, AbstractRegistrate<?> sourceReg, ResourceLocation sourceId) {
        Pair<Block, DisplaySource> pair = Pair.of(null, null);

        if (BuiltInRegistries.BLOCK.containsKey(blockId)) {
            pair.setFirst(BuiltInRegistries.BLOCK.get(blockId));
        } else {
            blockReg.addRegisterCallback(blockId.getPath(), Registries.BLOCK, block -> {
                pair.setFirst(block);
                checkAndAssignSource(pair);
            });
        }

        if (CreateBuiltInRegistries.DISPLAY_SOURCE.containsKey(blockId)) {
            pair.setSecond(CreateBuiltInRegistries.DISPLAY_SOURCE.get(sourceId));
        } else {
            sourceReg.addRegisterCallback(sourceId.getPath(), CreateRegistries.DISPLAY_SOURCE, source -> {
                pair.setSecond(source);
                checkAndAssignSource(pair);
            });
        }

        checkAndAssignSource(pair);
    }

    public static void init() {
        assignSource(
          CreateAccessor.getRegistrate(), AllBlocks.TRACK_STATION.getId(),
          CreateRailwaysNavigator.REGISTRATE, ADVANCED_DISPLAY_SOURCE.getId()
        );
    }
}
