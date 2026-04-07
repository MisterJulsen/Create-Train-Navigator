package de.mrjulsen.crn.util;

import java.util.List;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainStopDisplayData;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import org.jetbrains.annotations.Nullable;

public class VariableManager {
    private static String handleStopover(List<TrainStopDisplayData> list, int i, String variable) {
        if (i < 0 || i >= list.size()) return handleStopover(null, variable);
        return handleStopover(list.get(i), variable);
    }
    
    private static String handleStopover(@Nullable TrainStopDisplayData data, String variable) {
        boolean eta = variable.endsWith("_eta");

        // this should only return null if the variable is invalid so don't anyone
        // dare replace the tertiary statements with a single guard statement
        // - C1200
        return switch (variable) {
            case "station_name" -> data == null ? "" : data.getRealTimeStation().tagName();
            case "platform" -> data == null ? "" : data.getRealTimeStation().info().platform();
            case "arrival", "arrival_eta" -> data == null ? "" : ModUtils.formatTime(data.getScheduledArrivalTime(), eta);
            case "departure", "departure_eta" -> data == null ? "" : ModUtils.formatTime(data.getScheduledDepartureTime(), eta);
            default -> null;
        };
    }

    private static String handleTrainEntry(List<StationDisplayData> list, int i, String variable) {
        if (i >= list.size()) return handleTrainEntry(null, variable);
        return handleTrainEntry(list.get(i), variable);
    }

    private static String handleTrainEntry(@Nullable StationDisplayData data, String variable) {
        ITrainStopTypeSetting.ETrainStopType t = ITrainStopTypeSetting.ETrainStopType.ALL;

        // same rule as above applies

        String maybeReplacement = handleStopover(data == null ? null : data.getStationData(), variable);
        if (maybeReplacement != null) return maybeReplacement;

        if (variable.matches("^stop\\d+$")) {
            int n = Integer.parseInt(variable.substring(4));
            if (data == null || n < 0 || n >= data.getStopovers().size()) return "";
            return data.getStopovers().get(n);
        }

        return switch (variable) {
            case "via" -> data == null ? "" : data.getStopovers().stream().reduce("", (a, b) -> a + ", " + b);
            case "line" -> data == null ? "" : data.getTrainData().getName(ITrainStopTypeSetting.resolveStopState(data, t.showDepartures(data.isFirstStop()), t.showArrivals(data.isLastStop())));
            case "origin" -> data == null ? "" : data.getFirstStopName();
            case "destination" -> data == null ? "" : data.getStationData().getDestination();
            case "carriages" -> data == null ? "" : "" + data.getTrainData().getCarriages();
            case "delay_time" -> data == null ? "" : getDelayText(data);
            case "delay_reason" -> {
                if (data == null || data.getTrainData() == null) yield "";

                yield data.getTrainData().getStatus().stream().map(s -> s.text().getString()).filter(s -> !s.isBlank()).findFirst().orElse("");
            }
            default -> null;
        };
    }

    private static String getDelayText(StationDisplayData data) {
        var stop = data.getStationData();

        if (stop != null) {
            long ticks = stop.getDepartureTimeDeviation();

            if (ticks > 0) {
                String formatted = formatDelay(ticks);
                return CustomLanguage.translate("block.createrailwaysnavigator.advanced_display.ber.delayed", formatted).getString();
            }
        }

        return "";
    }

    private static String formatDelay(long ticks) {
        // similar logic used as in the DynamicDelayCondition class
        if (ticks <= 0) return "";

        boolean showInMinutes = ticks >= 20 * 60;

        int num = (int)(
                showInMinutes
                        ? Math.floor(ticks / (20 * 60f))
                        : Math.ceil(ticks / 100f) * 5
        );

        if (!showInMinutes) { return num + "s"; }
        if (num < 60) { return num + " min"; }

        int h = num / 60;
        int m = num % 60;

        return m > 0 ? h + "h " + m + " min" : h + "h";
    }

    private static String getReplacement(AdvancedDisplayBlockEntity blockEntity, String variable) {
        // generic
        DLTime time = new DLTime(DragonLib.getCurrentWorldTime(), DLTime.defaultTimeSystem());
        if (variable.equals("time")) {
            return time.format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
        } else if (variable.equals("day")) {
            return "" + (long)time.toGameDays(DLTime.defaultTimeSystem());
        }

        // on-board
        if (blockEntity.assembledOnContraption) {
            if (blockEntity.getElevatorData() != null) {
                // elevator
                if (variable.equals("elevator.current.short")) {
                    // Used only because the format %elevator.current.short% %elevator.current.sign% fails because short is often an integer.
                    return blockEntity.getElevatorData().currentShortName() + "\u200C";
                } else if (variable.equals("elevator.current.long")) {
                    return blockEntity.getElevatorData().currentLongName() + "\u200C";
                } else if (variable.equals("elevator.destination.short")) {
                    return blockEntity.getElevatorData().destinationShortName() + "\u200C";
                } else if (variable.equals("elevator.destination.long")) {
                    return blockEntity.getElevatorData().destinationLongName() + "\u200C";
                } else if (variable.equals("elevator.sign")) {
                    return blockEntity.getElevatorData().sign().getArrow();
                } else if (variable.equals("elevator.sign.triangle")) {
                    return blockEntity.getElevatorData().sign().getTriangle();
                } 
            }

            TrainDisplayData train = blockEntity.getTrainData();

            if (variable.equals("via")) {
                return train.getStopovers().stream().reduce("", (a, b) -> a + ", " + b.getRealTimeStation().tagName(), (a, b) -> a + ", " + b);
            } else if (variable.equals("line")) {
                return train.getTrainData().getName(ETrainStopState.beforeArrival(!train.isWaitingAtStation()));
            } else if (variable.equals("carriages")) {
                return "" + train.getTrainData().getCarriages();
            } else if (variable.startsWith("origin.")) {
                return handleStopover(train.getAllStops(), 0, variable.substring(7));
            } else if (variable.startsWith("destination.")) {
                return handleStopover(train.getAllStops(), train.getAllStops().size() - 1, variable.substring(12));
            } else if (variable.startsWith("next.")) {
                if (train.getStopovers().isEmpty())
                    return handleStopover(train.getAllStops(), train.getAllStops().size() - 1, variable.substring(5));
                return handleStopover(train.getStopovers(), 0, variable.substring(5));
            } else if (variable.matches("^stop\\d+\\..+")) {
                int dot = variable.indexOf(".");
                int n = Integer.parseInt(variable.substring(4, dot));
                return handleStopover(train.getStopovers(), n, variable.substring(dot + 1));
            }
        }

        // station
        if (!blockEntity.assembledOnContraption) {
            if (variable.matches("^train\\d+\\..+")) {
                int dot = variable.indexOf(".");
                int n = Integer.parseInt(variable.substring(5, dot));
                return handleTrainEntry(blockEntity.getStops(), n, variable.substring(dot + 1));
            }
        }

        return null;
    }

    public static String replacePlaceholders(String text, AdvancedDisplayBlockEntity blockEntity) {
        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // escaped placeholder \%
            if (c == '\\' && i + 1 < length && text.charAt(i + 1) == '%') {
                result.append('%');
                i++;
                continue;
            }

            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end > i + 1) {
                    String key = text.substring(i + 1, end);
                    String replacement = getReplacement(blockEntity, key);
                    if (replacement != null) {
                        result.append(replacement);
                    } else {
                        // Key nicht gefunden: lasse Platzhalter unverändert
                        result.append('%').append(key).append('%');
                    }
                    i = end;
                    continue;
                }
            }
            result.append(c);
        }

        return result.toString();
    }


    /*
    public static boolean hasValidPlaceholders(String text) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < length && text.charAt(i + 1) == '%') {
                i++;
                continue;
            }

            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end == -1) {
                    return false;
                }
                if (end == i + 1) {
                    return false;
                }
                String key = text.substring(i + 1, end);
                if (!variables.containsKey(key)) {
                    return false;
                }
                i = end;
            }
        }
        return true;
    }
    */
}
