package de.mrjulsen.crn.util;

import java.util.function.Supplier;

import com.simibubi.create.content.trains.entity.Train;

public final class RedSignalBarrier {

    private static final ThreadLocal<Train> SUBJECT = new ThreadLocal<>();

    private RedSignalBarrier() {}

    public static boolean appliesTo(Train train) {
        return train != null && SUBJECT.get() == train;
    }

    public static <T> T run(Train train, boolean enabled, Supplier<T> action) {
        if (!enabled || train == null) {
            return action.get();
        }
        Train previous = SUBJECT.get();
        SUBJECT.set(train);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    public static <T> T without(Supplier<T> action) {
        Train previous = SUBJECT.get();
        SUBJECT.remove();
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    private static void restore(Train previous) {
        if (previous == null) {
            SUBJECT.remove();
        } else {
            SUBJECT.set(previous);
        }
    }
}
