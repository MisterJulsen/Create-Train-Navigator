package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.ISavableNavigatorData;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

/**
 * A saved {@link RouteJourney} as the saved-routes list shows it.
 * <p>
 * Presentation only: the journey itself stays a plain travel plan and knows nothing about icons,
 * translations or the traveller's clock. Everything here is derived from it on the spot, so a list
 * entry redrawn later reflects the time it is drawn at rather than the time it was saved.
 */
public record SavedRouteData(RouteJourney journey) implements ISavableNavigatorData {

    private static final String KEY_PREFIX = "gui." + CreateRailwaysNavigator.MOD_ID + ".";

    @Override
    public List<SavableNavigatorDataLine> getOverviewData() {
        List<SavableNavigatorDataLine> lines = new ArrayList<>();

        lines.add(new SavableNavigatorDataLine(
            TextUtils.text(clockTime(scheduledDeparture()) + "   " + journey.origin().displayName()),
            icon(ModGuiIcons.ROUTE_START)));
        lines.add(new SavableNavigatorDataLine(
            TextUtils.text(clockTime(scheduledArrival()) + "   " + journey.destination().displayName()),
            icon(ModGuiIcons.ROUTE_END)));
        lines.add(new SavableNavigatorDataLine(
            TextUtils.translate(KEY_PREFIX + "route_overview.date", gameDay(scheduledDeparture()), clockTime(scheduledDeparture()))
                .append(" | ").append(duration(departureIn())),
            icon(ModGuiIcons.CALENDAR)));
        lines.add(new SavableNavigatorDataLine(
            TextUtils.translate(KEY_PREFIX + "route_overview.transfers", journey.transferCount())
                .append(" | ").append(duration(journey.duration())),
            icon(ModGuiIcons.INFO)));

        if (journey.isAnyCancelled()) {
            lines.add(new SavableNavigatorDataLine(
                TextUtils.translate(KEY_PREFIX + "route_overview.cancelled").withStyle(ChatFormatting.RED),
                icon(ModGuiIcons.IMPORTANT)));
        }
        return lines;
    }

    @Override
    public SavableNavigatorDataLine getTitle() {
        return new SavableNavigatorDataLine(TextUtils.translate(KEY_PREFIX + "saved_routes.saved_route"), icon(ModGuiIcons.BOOKMARK));
    }

    @Override
    public long timeOrderValue() {
        return scheduledDeparture();
    }

    /** When the timetable has the traveller leave, which is what the list groups and sorts by. */
    private long scheduledDeparture() {
        return journey.firstLeg().boarding().scheduled().departure();
    }

    private long scheduledArrival() {
        return journey.lastLeg().alighting().scheduled().arrival();
    }

    /** How long until the first train really leaves. Negative once it has gone. */
    private long departureIn() {
        return journey.departure() - ModUtils.getTransformedWorldTime();
    }

    private static String clockTime(long ticks) {
        return new DLTime(ticks, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
    }

    private static long gameDay(long ticks) {
        return (long) new DLTime(ticks, VanillaTimeSystem.INSTANCE).toGameDays(DLTime.defaultTimeSystem());
    }

    private static MutableComponent duration(long ticks) {
        return TextUtils.text(new DLTime(ticks, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME, DLTime.defaultTimeSystem()));
    }

    private static DLSprite icon(ModGuiIcons icon) {
        return icon.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE);
    }
}
