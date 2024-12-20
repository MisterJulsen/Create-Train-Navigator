package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class DLListUtils {
    public static <T> void iterateLooped(List<T> list, int startIndex, BiConsumer<Integer, T> action) {
        for (int i = 0; i < list.size(); i++) {
            int j = (i + startIndex) % list.size();
            action.accept(j, list.get(j));
        }
    }

    public static <T> List<T> getNextN(List<T> list, int startIndex, int count) {
        if (count > list.size()) {
            throw new IndexOutOfBoundsException("The number of elements to be obtained is greater than the list.");
        }
        List<T> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int j = (i + startIndex) % list.size();
            elements.add(list.get(j));
        }
        return elements;
    }
    
    public static <T> Optional<T> getNext(List<T> list, int startIndex, BiPredicate<Integer, T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            int j = (i + startIndex) % list.size();
            if (predicate.test(j, list.get(j))) {
                return Optional.of(list.get(j));
            }
        }
        return Optional.empty();
    }

    public static <T> Optional<T> getPrevious(List<T> list, int startIndex, BiPredicate<Integer, T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            int j = (i + startIndex) % list.size();
            if (predicate.test(j, list.get(j))) {
                return Optional.of(list.get(j));
            }
        }
        return Optional.empty();
    }
}
