package de.mrjulsen.crn.web.api;

import java.util.Optional;

public enum ApiVersion {

    V1(1, "1.0.0");

    private final int majorVersion;
    private final String minorVersion;

    ApiVersion(int majorVersion, String minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public String slug() {
        return String.format("v%s", majorVersion);
    }

    public int majorVersion() {
        return majorVersion;
    }

    public String minorVersion() {
        return minorVersion;
    }

    public String version() {
        return String.format("%s.%s", majorVersion, minorVersion);
    }

    public static Optional<ApiVersion> fromSlug(String slug) {
        for (ApiVersion version : values()) {
            if (version.slug().equalsIgnoreCase(slug)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    public static ApiVersion latest() {
        return values()[values().length - 1];
    }
}
