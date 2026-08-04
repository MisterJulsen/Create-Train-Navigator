package de.mrjulsen.crn.web.api;

import com.google.common.net.MediaType;

import java.util.Optional;

public enum ApiVersion {

    V1("v1", 1);

    private final String slug;
    private final int version;

    ApiVersion(String slug, int version) {
        this.slug = slug;
        this.version = version;
    }

    public String slug() {
        return slug;
    }

    public int version() {
        return version;
    }

    public static Optional<ApiVersion> fromSlug(String slug) {
        for (ApiVersion version : values()) {
            if (version.slug.equalsIgnoreCase(slug)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    public static ApiVersion latest() {
        return values()[values().length - 1];
    }
}
