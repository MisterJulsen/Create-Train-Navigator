package de.mrjulsen.crn.util;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import de.mrjulsen.crn.mixin.TrainPenaltyAccessor;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.mcdragonlib.util.Pair;

public class PenaltyResult {

    public enum Category { TRAINS, ROUTE; }
    public enum Type {
        MANUAL_TRAIN("manual_train", Category.TRAINS, TrainPenaltyAccessor.manualTrain()),
        IDLE_TRAIN("idle_train", Category.TRAINS, TrainPenaltyAccessor.idleTrain()),
        ARRIVING_TRAIN("arriving_train", Category.TRAINS, TrainPenaltyAccessor.arrivingTrain()),
        WAITING_TRAIN("waiting_train", Category.TRAINS, TrainPenaltyAccessor.waitingTrain()),
        ANY_TRAIN("any_train", Category.TRAINS, TrainPenaltyAccessor.anyTrain()),
        RED_SIGNAL("red_signal", Category.ROUTE, TrainPenaltyAccessor.redSignal()),
        REDSTONE_RED_SIGNAL("redstone_red_signal", Category.ROUTE, TrainPenaltyAccessor.redstoneRedSignal());

        private final String name;
        private final Category category;
        private final int penalty;

        private static final MapCache<Type, Pair<Category, Integer>, Pair<Category, Integer>> typeCaches = new MapCache<>((pair) -> {
            return Arrays.stream(values()).filter(x -> 
                (pair.getFirst() == null || x.getCategory() == pair.getFirst()) &&
                (x.getPenalty() == pair.getSecond())
            ).findFirst().orElse(switch (pair.getFirst()) {
                case TRAINS -> ANY_TRAIN;
                default -> null;
            });
        }, Pair::hashCode, ECachingPriority.LOW);

        private Type(String name, Category category, int penalty) {
            this.name = name;
            this.category = category;
            this.penalty = penalty;
        }

        public String getName() {
            return name;
        }

        public int getPenalty() {
            return penalty;
        }

        public Category getCategory() {
            return category;
        }

        public static Optional<Type> getTypeByPenalty(Category category, int penalty) {
            Pair<Category, Integer> p = Pair.of(category, penalty);
            return Optional.ofNullable(typeCaches.get(p, p));
        }
    }

    private final Map<Type, Integer> penaltiesByType;
    private final Cache<Integer> penaltySum;

    private PenaltyResult(Map<Type, Integer> penaltiesByType) {
        this.penaltiesByType = penaltiesByType;        
        this.penaltySum = new Cache<>(() -> getPenalties().values().stream().mapToInt(x -> x).sum());
    }

    public PenaltyResult() {
        this(new IdentityHashMap<>());
    }

    public PenaltyResult(PenaltyResult other) {
        this(new IdentityHashMap<>(other.penaltiesByType));
    }

    public ImmutableMap<Type, Integer> getPenalties(){
        return ImmutableMap.copyOf(penaltiesByType);
    }

    public int getPenaltyValue() {
        return this.penaltySum.get();
    }

    public void add(Type type) {
        penaltiesByType.merge(type, 1, (prev, val) -> prev + val);
    }    
}
