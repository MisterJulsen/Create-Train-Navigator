package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.block.display.properties.DepartureBoardDisplayTableSettings;
import de.mrjulsen.crn.block.display.properties.PassengerInformationDetailedSettings;
import de.mrjulsen.crn.block.display.properties.PassengerInformationScrollingTextSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayScrollingTextSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayTableSettings;
import de.mrjulsen.crn.block.display.properties.SimpleStaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.TrainDestinationCompactSettings;
import de.mrjulsen.crn.block.display.properties.TrainDestinationDetailedSettings;
import de.mrjulsen.crn.block.display.properties.TrainDestinationExtendedSettings;
import de.mrjulsen.crn.block.properties.EDisplayInfo;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayProperties;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.client.ber.variants.BERError;

public final class ModDisplayTypes {

    
    public static final DisplayTypeResourceKey PASSENGER_INFORMATION_RUNNING_TEXT = AdvancedDisplaysRegistry.register(
        EDisplayType.PASSENGER_INFORMATION, "running_text",
        PassengerInformationScrollingTextSettings::new, BERError::new, new DisplayProperties(true, null));
    
    public static final DisplayTypeResourceKey PASSENGER_INFORMATION_OVERVIEW = AdvancedDisplaysRegistry.register(
        EDisplayType.PASSENGER_INFORMATION, "detailed_with_schedule",
        PassengerInformationDetailedSettings::new, BERError::new, new DisplayProperties(false, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_SIMPLE = AdvancedDisplaysRegistry.register(
        EDisplayType.TRAIN_DESTINATION, "simple",
        TrainDestinationCompactSettings::new, BERError::new, new DisplayProperties(true, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_DETAILED = AdvancedDisplaysRegistry.register(
        EDisplayType.TRAIN_DESTINATION, "extended",
        TrainDestinationExtendedSettings::new, BERError::new, new DisplayProperties(true, null));
    
    public static final DisplayTypeResourceKey TRAIN_DESTINATION_OVERVIEW = AdvancedDisplaysRegistry.register(
        EDisplayType.TRAIN_DESTINATION, "detailed",
        TrainDestinationDetailedSettings::new, BERError::new, new DisplayProperties(true, null));
    
    public static final DisplayTypeResourceKey PLATFORM_RUNNING_TEXT = AdvancedDisplaysRegistry.register(
        EDisplayType.PLATFORM, "running_text",
        PlatformDisplayScrollingTextSettings::new, BERError::new, new DisplayProperties(true, be -> 16));
    
    public static final DisplayTypeResourceKey PLATFORM_TABLE = AdvancedDisplaysRegistry.register(
        EDisplayType.PLATFORM, "table",
        PlatformDisplayTableSettings::new, BERError::new, new DisplayProperties(false, be -> be.getYSize() * 3 - 1));
    
    public static final DisplayTypeResourceKey PLATFORM_FOCUS = AdvancedDisplaysRegistry.register(
        EDisplayType.PLATFORM, "focus",
        PlatformDisplayFocusSettings::new, BERError::new, new DisplayProperties(false, be -> be.getYSize() * 3 - 2));
        
    public static final DisplayTypeResourceKey DEPARTURE_BOARD_TABLE = AdvancedDisplaysRegistry.register(
        EDisplayType.DEPARTURE_BOARD, "table",
        DepartureBoardDisplayTableSettings::new, BERError::new, new DisplayProperties(false, be -> be.getYSize() * 3 - 2));

    public static final DisplayTypeResourceKey SIMPLE_TEXT = AdvancedDisplaysRegistry.register(
        EDisplayType.STATIC_TEXT, "simple_text",
        SimpleStaticTextDisplaySettings::new, BERError::new, new DisplayProperties(true, null));

    public static final DisplayTypeResourceKey RICH_TEXT = AdvancedDisplaysRegistry.register(
        EDisplayType.STATIC_TEXT, "rich_text",
        StaticTextDisplaySettings::new, BERError::new, new DisplayProperties(false, null));
    
    @Deprecated
    public static DisplayTypeResourceKey legacy_getKeyForType(EDisplayType type, EDisplayInfo info) {
        switch (type) {
            case PASSENGER_INFORMATION -> {
                return switch (info) {
                    case INFORMATIVE -> PASSENGER_INFORMATION_OVERVIEW;
                    default -> PASSENGER_INFORMATION_RUNNING_TEXT;
                };
            }
            case TRAIN_DESTINATION -> {
                return switch (info) {
                    case DETAILED -> TRAIN_DESTINATION_DETAILED;
                    case INFORMATIVE -> TRAIN_DESTINATION_OVERVIEW;
                    default -> TRAIN_DESTINATION_SIMPLE;
                };
            }
            case PLATFORM -> {
                return switch (info) {
                    case DETAILED -> PLATFORM_TABLE;
                    case INFORMATIVE -> PLATFORM_FOCUS;
                    default -> PLATFORM_RUNNING_TEXT;
                };
            }
            default -> { return TRAIN_DESTINATION_SIMPLE; }
        }
    }

    public static void init() {}
}
