package de.mrjulsen.crn.registry;

import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;

public class ModDisplayTags {
    public static void register() {
        PonderRegistry.TAGS.forTag(AllPonderTags.DISPLAY_TARGETS)
            .add(ModBlocks.ADVANCED_DISPLAY)
            .add(ModBlocks.ADVANCED_DISPLAY_BLOCK)
            .add(ModBlocks.ADVANCED_DISPLAY_PANEL)
            .add(ModBlocks.ADVANCED_DISPLAY_SMALL)
        ;
    }
}
