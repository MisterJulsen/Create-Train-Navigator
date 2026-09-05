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
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_PORT;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_THREADS;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_MAX_REQUEST_BYTES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_GZIP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_GZIP_MIN_BYTES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_REQUEST_LOG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEB_CORS_ORIGINS;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");

        ADVANCED_LOGGING = BUILDER.comment(new String[] {"Prints more details to the console to better observe the behavior of CRN. Only relevant for debugging."})
            .define("debug.advanced_logging", false);


        WEB_ENABLED = BUILDER.comment("Enables the CRN web API, which opens a separate port for external websites and tools to read train data over HTTP. This is a network service, so it stays off unless you really need it. (Default: OFF)")
            .define("web.enabled", false);
        WEB_BIND_ADDRESS = BUILDER.comment("The network interface the web API listens on which decides who can reach it. '127.0.0.1' = only the same machine (singleplayer, or behind a reverse proxy). '0.0.0.0' = every network, like your LAN and the internet if the port is open. (Default: 0.0.0.0)")
            .define("web.bind_address", "0.0.0.0");
        WEB_PORT = BUILDER.comment("The port the web API listens on over plain HTTP. It must not be the Minecraft port and must not be used by another program. Use a reverse proxy for HTTPS. (Default: 25580)")
            .defineInRange("web.port", 25580, 1, 65535);
        WEB_THREADS = BUILDER.comment("How many requests the web API can process at the same time. Higher values do not make the server faster, they only allow more clients to wait in parallel. (Default: 4)")
            .defineInRange("web.threads", 4, 1, 64);
        WEB_MAX_REQUEST_BYTES = BUILDER.comment("[in Bytes]", "The largest request body the web API accepts. Anything above is rejected with 413 before it is read into memory. (Default: 65536, 64 KiB)")
            .defineInRange("web.max_request_bytes", 65536, 1024, 8388608);
        WEB_GZIP_ENABLED = BUILDER.comment("Compresses responses with gzip when the client accepts it. This cuts the size of large responses. (Default: ON)")
            .define("web.gzip_enabled", true);
        WEB_GZIP_MIN_BYTES = BUILDER.comment("[in Bytes]", "Responses smaller than this are sent uncompressed, because gzipping a tiny payload costs more than it saves. Only used when gzip is enabled. (Default: 512)")
            .defineInRange("web.gzip_min_bytes", 512, 0, 1048576);
        WEB_REQUEST_LOG = BUILDER.comment("Writes one line to the server log for every request the web API handles, showing the method, path and response status. (Default: OFF)")
            .define("web.request_log", false);
        WEB_CORS_ORIGINS = BUILDER.comment("Which websites hosted elsewhere may read responses from this API in a browser. Use the exact origin including the scheme, for example 'https://trains.example.com'. A single '*' allows every website. (Default: none)")
            .defineList("web.cors_allowed_origins", List.of(), o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
