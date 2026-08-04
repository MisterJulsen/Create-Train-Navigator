package de.mrjulsen.crn.web;

import java.util.List;

import de.mrjulsen.crn.config.ModCommonConfig;

public record WebServerSettings(
    boolean enabled,
    String bindAddress,
    boolean httpEnabled,
    int httpPort,
    boolean httpsEnabled,
    int httpsPort,
    String keystorePath,
    String keystorePassword,
    int threads,
    int maxRequestBytes,
    boolean gzipEnabled,
    List<String> corsOrigins
) {

    public static WebServerSettings fromConfig() {
        return new WebServerSettings(
            ModCommonConfig.WEB_ENABLED.get(),
            ModCommonConfig.WEB_BIND_ADDRESS.get(),
            ModCommonConfig.WEB_HTTP_ENABLED.get(),
            ModCommonConfig.WEB_HTTP_PORT.get(),
            ModCommonConfig.WEB_HTTPS_ENABLED.get(),
            ModCommonConfig.WEB_HTTPS_PORT.get(),
            ModCommonConfig.WEB_KEYSTORE_PATH.get(),
            ModCommonConfig.WEB_KEYSTORE_PASSWORD.get(),
            ModCommonConfig.WEB_THREADS.get(),
            ModCommonConfig.WEB_MAX_REQUEST_BYTES.get(),
            ModCommonConfig.WEB_GZIP_ENABLED.get(),
            List.copyOf(ModCommonConfig.WEB_CORS_ORIGINS.get())
        );
    }
}
