package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.properties.EDisplayInfo;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeInfo;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoInformative;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoSimple;
import de.mrjulsen.crn.client.ber.variants.BERPlatformDetailed;
import de.mrjulsen.crn.client.ber.variants.BERPlatformInformative;
import de.mrjulsen.crn.client.ber.variants.BERPlatformSimple;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationDetailed;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationInformative;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationSimple;
import net.minecraft.resources.ResourceLocation;

public final class ModDisplayTypes {

    public static final DisplayTypeResourceKey PASSENGER_INFORMATION_RUNNING_TEXT = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.PASSENGER_INFORMATION, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "running_text"),
        BERPassengerInfoSimple::new, new DisplayTypeInfo(true, null));
    
    public static final DisplayTypeResourceKey PASSENGER_INFORMATION_OVERVIEW = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.PASSENGER_INFORMATION, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "detailed_with_schedule"),
        BERPassengerInfoInformative::new, new DisplayTypeInfo(false, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_SIMPLE = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.TRAIN_DESTINATION, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "simple"),
        BERTrainDestinationSimple::new, new DisplayTypeInfo(true, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_DETAILED = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.TRAIN_DESTINATION, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "extended"),
        BERTrainDestinationDetailed::new, new DisplayTypeInfo(true, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_OVERVIEW = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.TRAIN_DESTINATION, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "detailed"),
        BERTrainDestinationInformative::new, new DisplayTypeInfo(true, null));
    
    public static final DisplayTypeResourceKey PLATFORM_RUNNING_TEXT = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.PLATFORM, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "running_text"),
        BERPlatformSimple::new, new DisplayTypeInfo(true, be -> 32));
    
    public static final DisplayTypeResourceKey PLATFORM_TABLE = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.PLATFORM, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "table"),
        BERPlatformDetailed::new, new DisplayTypeInfo(false, be -> be.getYSize() * 3 - 1));
    
    public static final DisplayTypeResourceKey PLATFORM_FOCUS = AdvancedDisplaysRegistry.registerDisplayType(
        EDisplayType.PLATFORM, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "focus"),
        BERPlatformInformative::new, new DisplayTypeInfo(false, be -> be.getYSize() * 3 - 2));

    @Deprecated
    public static DisplayTypeResourceKey legacy_getKeyForType(EDisplayType type, EDisplayInfo info) {
        switch (type) {
            case PASSENGER_INFORMATION -> {
                switch (info) {
                    case INFORMATIVE -> { return PASSENGER_INFORMATION_OVERVIEW; }
                    default -> { return PASSENGER_INFORMATION_RUNNING_TEXT; }
                }
            }
            case TRAIN_DESTINATION -> {
                switch (info) {
                    case DETAILED -> { return TRAIN_DESTINATION_DETAILED; }
                    case INFORMATIVE -> { return TRAIN_DESTINATION_OVERVIEW; }
                    default -> { return TRAIN_DESTINATION_SIMPLE; }
                }
            }
            case PLATFORM -> {
                switch (info) {
                    case DETAILED -> { return PLATFORM_TABLE; }
                    case INFORMATIVE -> { return PLATFORM_FOCUS; }
                    default -> { return PLATFORM_RUNNING_TEXT; }
                }
            }
            default -> { return TRAIN_DESTINATION_SIMPLE; }
        }
    }

    public static void init() {}
}
