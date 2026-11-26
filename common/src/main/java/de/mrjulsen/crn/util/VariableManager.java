package de.mrjulsen.crn.util;

import java.util.List;
import java.util.function.Function;

import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.api.core.StationCall;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import org.jetbrains.annotations.Nullable;

public class VariableManager {
    private static <T> String join(List<T> items, Function<T, String> toString, String delimiter) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i != 0) string.append(delimiter);
            string.append(toString.apply(items.get(i)));
        }
        return string.toString();
    }

    private static String handleStopover(List<StopSnapshot> list, int i, String variable) {
        return handleCall(i < 0 || i >= list.size() ? null : list.get(i), variable);
    }

    /** The variables every station call answers, whatever it is a call of. */
    private static String handleCall(@Nullable StationCall data, String variable) {
        boolean eta = variable.endsWith("_eta");
        return switch (variable) {
            case "station_name" -> data == null ? "" : data.station().displayName();
            case "platform" -> data == null ? "" : data.platform();
            case "arrival", "arrival_eta" -> data == null ? "" : ModUtils.formatTime(data.scheduled().arrival(), eta);
            case "departure", "departure_eta" -> data == null ? "" : ModUtils.formatTime(data.scheduled().departure(), eta);
            case "delay_time" -> data == null || data.departureDeviation() <= 0 ? "" : formatDelay(data.departureDeviation());
            default -> null;
        };
    }

    private static String handleTrainEntry(List<BoardEntry> list, int i, String variable) {
        if (i < 0 || i >= list.size()) return handleTrainEntry(null, variable);
        return handleTrainEntry(list.get(i), variable);
    }

    private static String handleTrainEntry(@Nullable BoardEntry data, String variable) {
        if (variable.matches("^stop\\d+$")) {
            int n = Integer.parseInt(variable.substring(4));
            if (data == null || n < 0 || n >= data.stopovers().size()) return "";
            return data.stopovers().get(n).displayName();
        }

        if (variable.startsWith("via:")) {
            if (data == null) return "";
            return join(data.stopovers(), StationRef::displayName, variable.substring("via:".length()));
        }

        String shared = handleCall(data, variable);
        if (shared != null) {
            return shared;
        }

        return switch (variable) {
            case "via" -> data == null ? "" : join(data.stopovers(), StationRef::displayName, ", ");
            case "line" -> data == null ? "" : data.displayName(ITrainStopTypeSetting.resolveDirection(data, true, true));
            case "origin" -> data == null ? "" : data.origin().displayName();
            case "destination" -> data == null ? "" : data.destinationText();
            case "carriages" -> data == null ? "" : "" + data.carriageCount();
            case "delay_reason" -> data == null ? "" : data.primaryDelay()
                .map(x -> CustomLanguage.translate(x.translationKey(), x.displayArgs()).getString())
                .filter(s -> !s.isBlank())
                .orElse("");
            default -> null;
        };
    }

    private static String formatDelay(long ticks) {
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
        DLTime time = new DLTime(DragonLib.getCurrentWorldTime(), DLTime.defaultTimeSystem());
        if (variable.equals("time")) {
            return time.format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
        } else if (variable.equals("day")) {
            return "" + (long)time.toGameDays(DLTime.defaultTimeSystem());
        }

        if (blockEntity.assembledOnContraption) {
            if (blockEntity.getElevatorData() != null) {
                if (variable.equals("elevator.current.short")) {
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

            List<StopSnapshot> service = blockEntity.getServiceStops();
            List<StopSnapshot> stopovers = blockEntity.getStopovers();

            if (variable.equals("via")) {
                return join(stopovers, s -> s.station().displayName(), ", ");
            } else if (variable.startsWith("via:")) {
                return join(stopovers, s -> s.station().displayName(), variable.substring("via:".length()));
            } else if (variable.equals("line")) {
                return blockEntity.getTrainDisplayName();
            } else if (variable.equals("carriages")) {
                return "" + blockEntity.getTrain().map(TrainSnapshot::carriageCount).orElse(0);
            } else if (variable.equals("title")) {
                return blockEntity.getDestinationText();
            } else if (variable.startsWith("origin.")) {
                return handleStopover(service, 0, variable.substring(7));
            } else if (variable.startsWith("destination.")) {
                return handleStopover(service, service.size() - 1, variable.substring(12));
            } else if (variable.startsWith("next.")) {
                if (stopovers.isEmpty())
                    return handleStopover(service, service.size() - 1, variable.substring(5));
                return handleStopover(stopovers, 0, variable.substring(5));
            } else if (variable.matches("^stop\\d+\\..+")) {
                int dot = variable.indexOf(".");
                int n = Integer.parseInt(variable.substring(4, dot));
                return handleStopover(stopovers, n, variable.substring(dot + 1));
            }
        }

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
        return processPlaceholders(text, name -> getReplacement(blockEntity, name));
    }

    public static boolean hasVariables(String text) {
        boolean[] found = { false };
        processPlaceholders(text, name -> {
            found[0] = true;
            return null;
        });
        return found[0];
    }

    public static String unescapePlaceholders(String text) {
        return processPlaceholders(text, name -> null);
    }

    private static String processPlaceholders(String text, Function<String, String> resolver) {
        if (text == null || text.indexOf('%') < 0) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        int length = text.length();
        int i = 0;
        while (i < length) {
            char c = text.charAt(i);
            boolean openEscaped = c == '\\' && i + 1 < length && text.charAt(i + 1) == '%';
            if (c == '%' || openEscaped) {
                int nameStart = i + (openEscaped ? 2 : 1);
                int close = text.indexOf('%', nameStart);
                if (close >= 0) {
                    boolean closeEscaped = close > nameStart && text.charAt(close - 1) == '\\';
                    int nameEnd = closeEscaped ? close - 1 : close;
                    if (isVariableName(text, nameStart, nameEnd)) {
                        String name = text.substring(nameStart, nameEnd);
                        String replacement = openEscaped || closeEscaped ? null : resolver.apply(name);
                        result.append(replacement != null ? replacement : "%" + name + "%");
                        i = close + 1;
                        continue;
                    }
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    private static boolean isVariableName(String text, int start, int end) {
        if (end <= start) {
            return false;
        }
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == ':') {
                return i > start;
            }
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                return false;
            }
        }
        return true;
    }

}
