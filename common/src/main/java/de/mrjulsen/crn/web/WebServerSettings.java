package de.mrjulsen.crn.web;

import java.util.List;

import de.mrjulsen.crn.config.ModCommonConfig;

public record WebServerSettings(
    boolean enabled,
    String bindAddress,
    int port,
    int threads,
    int maxRequestBytes,
    boolean gzipEnabled,
    int gzipMinBytes,
    boolean requestLog,
    List<String> corsOrigins
) {

    public static WebServerSettings fromConfig() {
        return new WebServerSettings(
            ModCommonConfig.WEB_ENABLED.get(),
            ModCommonConfig.WEB_BIND_ADDRESS.get(),
            ModCommonConfig.WEB_PORT.get(),
            ModCommonConfig.WEB_THREADS.get(),
            ModCommonConfig.WEB_MAX_REQUEST_BYTES.get(),
            ModCommonConfig.WEB_GZIP_ENABLED.get(),
            ModCommonConfig.WEB_GZIP_MIN_BYTES.get(),
            ModCommonConfig.WEB_REQUEST_LOG.get(),
            List.copyOf(ModCommonConfig.WEB_CORS_ORIGINS.get())
        );
    }
}
