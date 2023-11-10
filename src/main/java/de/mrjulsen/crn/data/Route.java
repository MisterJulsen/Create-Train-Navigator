package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Route {
    private Collection<RoutePart> parts = new ArrayList<>();

    public Route(Collection<RoutePart> initialValues) {
        this.parts.addAll(initialValues);
    }

    public Route() {
        
    }

    public void addPart(RoutePart part) {
        if (!contains(part)) {
            parts.add(part);
        }
    }
    
    public boolean contains(RoutePart part) {
        return parts.contains(part);
    }

    public Collection<RoutePart> getParts() {
        return parts;
    }

    public int getStationCount() {
        return parts.stream().mapToInt(x -> x.getStationCount(false)).sum() + parts.size() + 1;
    }

    public int getTransferCount() {
        return getParts().size() - 1;
    }

    public int getTotalDuration() {
        return getEndStation().getPrediction().getTicks() - getStartStation().getPrediction().getTicks();
    }

    public TrainStop getStartStation() {
        return parts.stream().findFirst().get().getStartStation();
    }

    public TrainStop getEndStation() {
        return parts.stream().reduce((a, b) -> b).get().getEndStation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route other) {
            return getParts().size() == other.getParts().size() && getParts().stream().allMatch(x -> other.getParts().stream().anyMatch(y -> y.equals(x)));
        }
        return false;
    }

    public boolean exactEquals(Object obj, boolean respectOrder, boolean respectTrains) {
        if (obj instanceof Route other) {
            if (getParts().size() != other.getParts().size()) {
                return false;
            }

            RoutePart[] a = getParts().toArray(RoutePart[]::new);
            RoutePart[] b = other.getParts().toArray(RoutePart[]::new);

            for (int i = 0; i < a.length; i++) {
                if ((respectOrder && !a[i].exactEquals(b[i], respectTrains)) || (!respectOrder && !a[i].equals(b[i]))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String toName() {
        return String.format("%s - %s",
            parts.stream().findFirst().get().getStartStation().getStationAlias().getAliasName(),
            parts.stream().reduce((a, b) -> b).get().getEndStation().getStationAlias().getAliasName()
        );
    }

    @Override
    public String toString() {
        return Arrays.toString(getParts().toArray());
    }

    
    public int compareTo(Route o, UserSettings settings) {
        int offset = settings.getFilterCriteria().getId();
        for (int i = 0; i < EFilterCriteria.values().length; i++) {
            int j = (i + offset) % EFilterCriteria.values().length;

            EFilterCriteria criteria = EFilterCriteria.getCriteriaById(j);
            int a = EFilterCriteria.getDataFromRoute(criteria, this);
            int b = EFilterCriteria.getDataFromRoute(criteria, o);
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }
    
}
