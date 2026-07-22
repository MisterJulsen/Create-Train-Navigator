package de.mrjulsen.crn.cmd;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import de.mrjulsen.crn.backend.api.CategorySnapshot;
import de.mrjulsen.crn.backend.api.LineSnapshot;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.NavigationResult;
import de.mrjulsen.crn.navigator.NavigationStatus;
import de.mrjulsen.crn.navigator.Navigator;
import de.mrjulsen.crn.navigator.RouteOptimization;
import de.mrjulsen.crn.navigator.debug.NavigatorDiagnosticsDump;
import de.mrjulsen.crn.navigator.Waypoint;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.navigator.route.RouteTransfer;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Drives the route search from the chat, so it can be exercised without a screen in front of it.
 * <p>
 * The interesting part of a search is not the route but how it was arrived at, so this prints the
 * timings, the size of what was searched and every leg with its times, rather than a tidy summary.
 *
 * <pre>
 * /createrailwaysnavigator debug navigate "Berlin" "Munich"
 * /createrailwaysnavigator debug navigate "Berlin" "Munich" via=Leipzig; stay=6000; max=2
 * /createrailwaysnavigator debug navigate "Berlin" "Munich" direct; in=3000
 * </pre>
 *
 * Options are separated by {@code ;} and are either a flag or {@code key=value}:
 * <ul>
 *   <li>{@code in=<ticks>} / {@code at=<ticks>} - when to leave, from now or absolute</li>
 *   <li>{@code via=<station>} - travel via, repeatable; {@code stay=<ticks>} applies to the last one</li>
 *   <li>{@code avoid=<station>} - do not use this station, repeatable</li>
 *   <li>{@code transfer=<ticks>}, {@code max=<changes>}, {@code results=<n>}, {@code horizon=<ticks>}</li>
 *   <li>{@code line=<name>} / {@code !line=<name>}, {@code cat=<name>} / {@code !cat=<name>}</li>
 *   <li>{@code direct} - only routes without a change</li>
 *   <li>{@code comfort} - present the route with the fewest changes first</li>
 * </ul>
 */
public final class NavigatorDebugCommand {

    private static final String SUB_NAVIGATE = "navigate";
    private static final String SUB_INDEX = "navigatorIndex";
    private static final String SUB_DUMP = "navigatorDump";

    private static final String ARG_FROM = "from";
    private static final String ARG_TO = "to";
    private static final String ARG_OPTIONS = "options";

    private NavigatorDebugCommand() {}

    /** The {@code navigate} subcommand, to be attached to the mod's debug command. */
    public static LiteralArgumentBuilder<CommandSourceStack> navigate() {
        RequiredArgumentBuilder<CommandSourceStack, String> to = Commands.argument(ARG_TO, StringArgumentType.string())
            .suggests((context, builder) -> suggestStations(builder))
            .executes(x -> run(x, ""))
            .then(Commands.argument(ARG_OPTIONS, StringArgumentType.greedyString())
                .executes(x -> run(x, StringArgumentType.getString(x, ARG_OPTIONS))));

        return Commands.literal(SUB_NAVIGATE)
            .then(Commands.argument(ARG_FROM, StringArgumentType.string())
                .suggests((context, builder) -> suggestStations(builder))
                .then(to));
    }

    /**
     * Offers station names already in quotes, since most of them contain spaces and the argument
     * would otherwise end at the first one.
     * <p>
     * The completion has to do the quoting rather than leave it to whoever is typing: a suggestion
     * inserted as bare text turns a station into two arguments and the command stops parsing where
     * the name begins. Matching ignores the opening quote the player has usually typed by then, so a
     * name can be found by typing into it either way.
     */
    private static CompletableFuture<Suggestions> suggestStations(SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        if (typed.startsWith("\"") || typed.startsWith("'")) {
            typed = typed.substring(1);
        }
        String lowercase = typed.toLowerCase(Locale.ROOT);

        for (String station : stationNames()) {
            if (station.toLowerCase(Locale.ROOT).contains(lowercase)) {
                builder.suggest(quote(station));
            }
        }
        return builder.buildFuture();
    }

    /** A station name as a single command argument, whatever characters it contains. */
    private static String quote(String name) {
        return '"' + name.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /** The {@code navigatorIndex} subcommand, which reports and drops the cached timetable index. */
    public static LiteralArgumentBuilder<CommandSourceStack> index() {
        return Commands.literal(SUB_INDEX).executes(x -> showIndex(x.getSource()));
    }

    /**
     * The {@code navigatorDump} subcommand, which writes everything the search works from to a file.
     * Given a station pair it runs that query as well and records what came of it.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> dump() {
        RequiredArgumentBuilder<CommandSourceStack, String> to = Commands.argument(ARG_TO, StringArgumentType.string())
            .suggests((context, builder) -> suggestStations(builder))
            .executes(x -> writeDump(x, ""))
            .then(Commands.argument(ARG_OPTIONS, StringArgumentType.greedyString())
                .executes(x -> writeDump(x, StringArgumentType.getString(x, ARG_OPTIONS))));

        return Commands.literal(SUB_DUMP)
            .executes(x -> writeDump(x.getSource(), null))
            .then(Commands.argument(ARG_FROM, StringArgumentType.string())
                .suggests((context, builder) -> suggestStations(builder))
                .then(to));
    }

    private static int writeDump(CommandContext<CommandSourceStack> context, String options) {
        NavigationQuery query;
        try {
            query = parse(StringArgumentType.getString(context, ARG_FROM),
                StringArgumentType.getString(context, ARG_TO), options);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(TextUtils.text(e.getMessage()));
            return 0;
        }
        return writeDump(context.getSource(), query);
    }

    private static int writeDump(CommandSourceStack source, NavigationQuery query) {
        if (!RailwayBackendApi.isActive()) {
            source.sendFailure(TextUtils.text("The railway backend is not running."));
            return 0;
        }

        return NavigatorDiagnosticsDump.write(query)
            .map(file -> {
                source.sendSuccess(() -> TextUtils.text("Navigator diagnostics written to " + file), false);
                return 1;
            })
            .orElseGet(() -> {
                source.sendFailure(TextUtils.text("Unable to write the navigator diagnostics dump."));
                return 0;
            });
    }

    private static Iterable<String> stationNames() {
        return RailwayBackendApi.isActive() ? RailwayBackendApi.getKnownStations() : List.of();
    }

    private static int showIndex(CommandSourceStack source) {
        if (!RailwayBackendApi.isActive()) {
            source.sendFailure(TextUtils.text("The railway backend is not running."));
            return 0;
        }

        long now = RailwayBackendApi.currentTime();
        TimetableIndex index = TimetableIndex.build(now, now + NavigationQuery.DEFAULT_SEARCH_HORIZON);
        TimetableIndex.invalidate();

        source.sendSuccess(() -> TextUtils.text(String.format(
            "Timetable index rebuilt in %dms: %d station nodes, %d trips over %d ticks. Cache dropped.",
            index.buildDurationMs(), index.nodeCount(), index.tripCount(), index.until() - index.from())), false);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> context, String options) {
        CommandSourceStack source = context.getSource();
        NavigationQuery query;
        try {
            query = parse(StringArgumentType.getString(context, ARG_FROM),
                StringArgumentType.getString(context, ARG_TO), options);
        } catch (IllegalArgumentException e) {
            source.sendFailure(TextUtils.text(e.getMessage()));
            return 0;
        }

        NavigationResult result = Navigator.search(query);

        if (result.status() == NavigationStatus.UNKNOWN_STATION) {
            reportUnknownStations(source, query);
            return 0;
        }

        source.sendSuccess(() -> TextUtils.text(String.format("§7--- %s -> %s%s ---",
            query.origin(), query.destination(),
            query.hasWaypoints() ? " via " + query.waypoints() : "")), false);
        source.sendSuccess(() -> TextUtils.text(String.format(
            "§7%s in §f%dms§7, %d nodes, %d trips ridden, departing after %s",
            result.status(), result.durationMs(), result.stationsSearched(), result.tripsScanned(),
            clock(query.resolvedDepartAfter()))), false);

        if (result.isEmpty()) {
            return result.status().isSuccess() ? 1 : 0;
        }

        int number = 1;
        for (RouteJourney journey : result.journeys()) {
            printJourney(source, journey, number++);
        }
        return 1;
    }

    /**
     * Says which of the named places could not be found, and what was probably meant.
     * <p>
     * A name that lost everything after its first space parses as a perfectly valid argument and
     * fails only later, as a station nobody has heard of - so saying which name failed, and offering
     * the ones that start with it, is usually the whole answer.
     */
    private static void reportUnknownStations(CommandSourceStack source, NavigationQuery query) {
        List<String> named = new ArrayList<>();
        named.add(query.origin());
        named.add(query.destination());
        query.waypoints().forEach(x -> named.add(x.station()));

        for (String name : named) {
            if (isKnown(name)) {
                continue;
            }
            List<String> similar = new ArrayList<>();
            for (String station : stationNames()) {
                if (station.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                    similar.add(quote(station));
                }
            }
            source.sendFailure(TextUtils.text(similar.isEmpty()
                ? "No station or tag named '" + name + "'."
                : "No station or tag named '" + name + "'. Did you mean " + String.join(", ", similar) + "?"));
        }
    }

    /** Whether a name refers to something the search can start from or travel to. */
    private static boolean isKnown(String name) {
        Set<String> stations = RailwayBackendApi.getKnownStations();
        if (stations.contains(name)) {
            return true;
        }
        boolean tagged = RailwayBackendApi.getAllStationTags().stream()
            .anyMatch(x -> x.getTagName().get().equalsIgnoreCase(name));
        return tagged || stations.stream().anyMatch(x -> TrainUtils.stationMatches(x, name));
    }

    private static void printJourney(CommandSourceStack source, RouteJourney journey, int number) {
        source.sendSuccess(() -> TextUtils.text(String.format(
            "§e#%d §f%s §7-> §f%s  §7(%s, %d change%s%s)",
            number, clock(journey.departure()), clock(journey.arrival()), duration(journey.duration()),
            journey.transferCount(), journey.transferCount() == 1 ? "" : "s",
            journey.hasRiskyTransfer() ? ", §c" + journey.worstTransferRisk() + "§7" : "")), false);

        List<RouteLeg> legs = journey.legs();
        for (int i = 0; i < legs.size(); i++) {
            RouteLeg leg = legs.get(i);
            source.sendSuccess(() -> TextUtils.text(String.format(
                "  §b%s §7-> %s §8| §f%s %s §7-> §f%s %s §8(%d stop%s)",
                leg.displayName(), leg.destinationText(),
                clock(leg.departure()), platform(leg.boarding()),
                clock(leg.arrival()), platform(leg.alighting()),
                leg.intermediateStopCount(), leg.intermediateStopCount() == 1 ? "" : "s")), false);

            if (i < journey.transfers().size()) {
                RouteTransfer transfer = journey.transfers().get(i);
                source.sendSuccess(() -> TextUtils.text(String.format(
                    "    §7%s at %s (%s)%s",
                    transfer.staysSeated() ? "stay seated" : "change",
                    transfer.arrivalStationName(), duration(transfer.duration()),
                    transfer.state().isWarning() ? " §c[" + transfer.state() + "]" : "")), false);
            }
        }
    }

    private static String platform(RouteCall call) {
        return call.realtimeStation().hasPlatform() ? "§7[" + call.realtimePlatform() + "]§f" : "";
    }

    private static String clock(long ticks) {
        long day = Math.floorMod(ticks, 24000L);
        return String.format("%02d:%02d", Math.floorDiv(day, 1000L), (day % 1000L) * 60 / 1000);
    }

    private static String duration(long ticks) {
        long seconds = ticks / 20;
        return seconds >= 60 ? String.format("%dm%02ds", seconds / 60, seconds % 60) : seconds + "s";
    }

    private static NavigationQuery parse(String from, String to, String options) {
        NavigationQuery query = NavigationQuery.from(from).to(to);
        Set<String> avoided = new LinkedHashSet<>();
        Set<UUID> includedLines = new LinkedHashSet<>();
        Set<UUID> excludedLines = new LinkedHashSet<>();
        Set<UUID> includedCategories = new LinkedHashSet<>();
        Set<UUID> excludedCategories = new LinkedHashSet<>();
        List<Waypoint> waypoints = new ArrayList<>();

        for (String token : options.split(";")) {
            String option = token.trim();
            if (option.isEmpty()) {
                continue;
            }

            int separator = option.indexOf('=');
            String key = (separator < 0 ? option : option.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
            String value = separator < 0 ? "" : option.substring(separator + 1).trim();

            switch (key) {
                case "direct" -> query = query.onlyDirect();
                case "comfort" -> query = query.preferring(RouteOptimization.FEWEST_TRANSFERS);
                case "in" -> query = query.departingIn(number(key, value));
                case "at" -> query = query.departingAfter(number(key, value));
                case "transfer" -> query = query.withMinTransferTime(number(key, value));
                case "max" -> query = query.withMaxTransfers((int) number(key, value));
                case "results" -> query = query.withMaxResults((int) number(key, value));
                case "horizon" -> query = query.withSearchHorizon(number(key, value));
                case "via" -> waypoints.add(Waypoint.of(value));
                case "stay" -> waypoints.add(withStay(waypoints, number(key, value)));
                case "avoid" -> avoided.add(value);
                case "line" -> includedLines.add(lineId(value));
                case "!line" -> excludedLines.add(lineId(value));
                case "cat" -> includedCategories.add(categoryId(value));
                case "!cat" -> excludedCategories.add(categoryId(value));
                default -> throw new IllegalArgumentException("Unknown option '" + key + "'.");
            }
        }

        return query.withWaypoints(waypoints)
            .avoidingStations(avoided)
            .onlyLines(includedLines)
            .excludingLines(excludedLines)
            .onlyCategories(includedCategories)
            .excludingCategories(excludedCategories);
    }

    /** Replaces the most recently named waypoint with one the traveller wants time at. */
    private static Waypoint withStay(List<Waypoint> waypoints, long stay) {
        if (waypoints.isEmpty()) {
            throw new IllegalArgumentException("'stay' has to follow a 'via'.");
        }
        return Waypoint.of(waypoints.remove(waypoints.size() - 1).station(), stay);
    }

    private static long number(String key, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + key + "' expects a number, got '" + value + "'.");
        }
    }

    private static UUID lineId(String name) {
        return RailwayBackendApi.getAllLines().stream()
            .filter(x -> x.name().equalsIgnoreCase(name))
            .map(LineSnapshot::id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No train line named '" + name + "'."));
    }

    private static UUID categoryId(String name) {
        return RailwayBackendApi.getAllCategories().stream()
            .filter(x -> x.name().equalsIgnoreCase(name))
            .map(CategorySnapshot::id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No train category named '" + name + "'."));
    }
}
