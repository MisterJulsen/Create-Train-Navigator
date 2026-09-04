package de.mrjulsen.crn.config;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ADVANCED_LOGGING;

    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_BIND_ADDRESS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_HTTP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_HTTP_PORT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_HTTPS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_HTTPS_PORT;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_KEYSTORE_PATH;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_KEYSTORE_PASSWORD;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_THREADS;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_MAX_REQUEST_BYTES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_GZIP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEB_CORS_ORIGINS;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");

        ADVANCED_LOGGING = BUILDER.comment(new String[] {"Prints more details to the console to better observe the behavior of CRN. Only relevant for debugging."})
            .define("debug.advanced_logging", false);


        WEB_ENABLED = BUILDER.comment("Enables the CRN web API, which opens a separate port for external websites and tools to read train data over HTTP.", "This is a network service, so it stays off unless you deliberately want it. (Default: OFF)")
            .define("web.enabled", false);
        WEB_BIND_ADDRESS = BUILDER.comment("The network interface the web API listens on which decides who can reach it.", "'127.0.0.1' = only the same machine (singleplayer, or behind a reverse proxy). '0.0.0.0' = every network, like your LAN and the internet if the port is open. (Default: 0.0.0.0)")
            .define("web.bind_address", "0.0.0.0");
        WEB_HTTP_ENABLED = BUILDER.comment("Serves the API over plain HTTP. Traffic is not encrypted, so do not expose this to the internet without a reverse proxy in front of it. (Default: ON)")
            .define("web.http.enabled", true);
        WEB_HTTP_PORT = BUILDER.comment("The port of the plain HTTP listener. It must not be the Minecraft port and must not be used by another program. (Default: 25580)")
            .defineInRange("web.http.port", 25580, 1, 65535);
        WEB_HTTPS_ENABLED = BUILDER.comment("Serves the API over HTTPS. Requires a keystore; see web.https.keystore_path. (Default: OFF)")
            .define("web.https.enabled", false);
        WEB_HTTPS_PORT = BUILDER.comment("The port of the HTTPS listener. (Default: 25585)")
            .defineInRange("web.https.port", 25585, 1, 65535);
        WEB_KEYSTORE_PATH = BUILDER.comment("Path to a PKCS#12 keystore holding the certificate and private key for HTTPS, relative to the game directory.", "Empty means HTTPS stays off even when it is enabled above.")
            .define("web.https.keystore_path", "");
        WEB_KEYSTORE_PASSWORD = BUILDER.comment("Password of the keystore above. Stored in plain text, so keep this config file readable only by the server operator.")
            .define("web.https.keystore_password", "");
        WEB_THREADS = BUILDER.comment("How many requests the web API can process at the same time. Higher values do not make the server faster, they only allow more clients to wait in parallel. (Default: 4)")
            .defineInRange("web.threads", 4, 1, 64);
        WEB_MAX_REQUEST_BYTES = BUILDER.comment("[in Bytes] The largest request body the web API accepts. Anything above is rejected with 413 before it is read into memory. (Default: 65536, 64 KiB)")
            .defineInRange("web.max_request_bytes", 65536, 1024, 8388608);
        WEB_GZIP_ENABLED = BUILDER.comment("Compresses responses with gzip when the client accepts it. JSON compresses very well, so this cuts the size of large responses a lot at a small CPU cost. (Default: ON)")
            .define("web.gzip_enabled", true);
        WEB_CORS_ORIGINS = BUILDER.comment("Which websites hosted elsewhere may read responses from this API in a browser.", "Use the exact origin including the scheme, for example 'https://trains.example.com'. A single '*' allows every website. (Default: none)")
            .defineList("web.cors_allowed_origins", List.of(), o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
