package de.mrjulsen.crn.web.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PathPattern {

    private record Segment(String value, boolean parameter) {}

    private final String raw;
    private final List<Segment> segments;

    private PathPattern(String raw, List<Segment> segments) {
        this.raw = raw;
        this.segments = segments;
    }

    public static PathPattern of(String path) {
        List<String> parts = split(path);
        List<Segment> segments = new ArrayList<>(parts.size());
        for (String part : parts) {
            if (part.length() >= 2 && part.startsWith("{") && part.endsWith("}")) {
                segments.add(new Segment(part.substring(1, part.length() - 1), true));
            } else {
                segments.add(new Segment(part, false));
            }
        }
        return new PathPattern(String.join("/", parts), segments);
    }

    public static List<String> split(String path) {
        List<String> out = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }

    public Optional<Map<String, String>> match(List<String> pathSegments) {
        if (pathSegments.size() != segments.size()) {
            return Optional.empty();
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            String value = pathSegments.get(i);
            if (segment.parameter()) {
                parameters.put(segment.value(), value);
            } else if (!segment.value().equals(value)) {
                return Optional.empty();
            }
        }
        return Optional.of(parameters);
    }

    public String raw() {
        return raw;
    }

    public int staticSegmentCount() {
        int count = 0;
        for (Segment segment : segments) {
            if (!segment.parameter()) {
                count++;
            }
        }
        return count;
    }
}
