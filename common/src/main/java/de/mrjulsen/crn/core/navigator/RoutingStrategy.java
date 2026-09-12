package de.mrjulsen.crn.core.navigator;

import java.util.Arrays;
import java.util.Comparator;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

/** How found journeys should be ordered, so the best one for the chosen goal comes first. */
public enum RoutingStrategy implements ITranslatableEnum {

    /** Prefers the journey that arrives earliest, then the one with fewer transfers. */
    FASTEST("fastest", Comparator
        .comparingLong(RouteJourney::arrival)
        .thenComparingInt(RouteJourney::transferCount)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed())),

    /** Prefers the journey with fewer transfers, then the one that arrives earliest. */
    FEWEST_TRANSFERS("fewest_transfers", Comparator
        .comparingInt(RouteJourney::transferCount)
        .thenComparingLong(RouteJourney::arrival)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed()));

    private final String id;
    private final Comparator<RouteJourney> comparator;


    RoutingStrategy(String id, Comparator<RouteJourney> comparator) {
        this.id = id;
        this.comparator = comparator;
    }

    public String getId() {
        return id;
    }

    public static RoutingStrategy fromId(String id) {
        return Arrays.stream(RoutingStrategy.values()).filter(rs -> rs.id.equals(id)).findFirst().orElse(RoutingStrategy.FASTEST);
    }

    /** The ordering this optimization applies, putting the best journey first. */
    public Comparator<RouteJourney> comparator() {
        return comparator;
    }

    @Override
    public Data getTranslationData() {
        return new Data(CreateRailwaysNavigator.MOD_ID, "routing_strategy", id);
    }
}
