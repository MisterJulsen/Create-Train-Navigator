package de.mrjulsen.crn.web;

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.api.ApiVersion;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.HttpMethod;

public final class WebServer {

    public static final String API_SEGMENT = "api";
    public static final List<String> NAMESPACES = List.of(
        CreateRailwaysNavigator.MOD_ID,
        CreateRailwaysNavigator.SHORT_MOD_ID
    );

    public static final int MIN_THREADS = 4;
    private static final long STOP_TIMEOUT_MILLIS = 2000L;

    private static WebServer instance;

    private final Server server;

    private WebServer(Server server) {
        this.server = server;
    }

    public static synchronized void start() {
        if (instance != null) {
            CreateRailwaysNavigator.LOGGER.warn("CRN web API is already running.");
            return;
        }
        WebServerSettings settings = WebServerSettings.fromConfig();
        if (!settings.enabled()) {
            CreateRailwaysNavigator.LOGGER.info("CRN web API is disabled in the config.");
            return;
        }

        Server server = build(settings);
        try {
            server.start();
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Failed to start the CRN web API.", e);
            stopQuietly(server);
            return;
        }
        instance = new WebServer(server);
        CreateRailwaysNavigator.LOGGER.info("CRN web API listening on {}:{}{}", settings.bindAddress(), settings.port(), basePathInfo());
        CreateRailwaysNavigator.LOGGER.info("CRN web API serving {} endpoint(s).", EndpointRegistry.size());
    }

    public static synchronized void stop() {
        if (instance == null) {
            return;
        }
        stopQuietly(instance.server);
        instance = null;
        CreateRailwaysNavigator.LOGGER.info("CRN web API stopped.");
    }

    private static Server build(WebServerSettings settings) {
        QueuedThreadPool threadPool = new QueuedThreadPool(Math.max(settings.threads(), MIN_THREADS));
        threadPool.setName("CRN Web");

        Server server = new Server(threadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(STOP_TIMEOUT_MILLIS);

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendXPoweredBy(false);

        ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory(httpConfig));
        connector.setHost(settings.bindAddress());
        connector.setPort(settings.port());
        connector.setIdleTimeout(settings.idleTimeoutMillis());
        server.addConnector(connector);

        server.setHandler(compression(settings, cors(settings, router(settings))));
        server.setErrorHandler(new JsonErrorHandler());
        return server;
    }

    private static Handler router(WebServerSettings settings) {
        List<ContextHandler> contexts = new ArrayList<>();
        for (ApiVersion version : ApiVersion.values()) {
            PathMappings<Map<HttpMethod, EndpointRegistry.Route>> mappings = EndpointRegistry.mappingsFor(version);
            for (String namespace : NAMESPACES) {
                String contextPath = "/" + namespace + "/" + API_SEGMENT + "/" + version.slug();
                contexts.add(new ContextHandler(new ApiHandler(settings, mappings), contextPath));
            }
        }
        return new ContextHandlerCollection(contexts.toArray(new ContextHandler[0]));
    }

    private static Handler compression(WebServerSettings settings, Handler next) {
        if (!settings.gzipEnabled()) {
            return next;
        }
        GzipCompression gzip = new GzipCompression();
        gzip.setMinCompressSize(settings.gzipMinBytes());

        CompressionConfig config = CompressionConfig.builder()
            .compressIncludeMethod(HttpMethod.GET.name())
            .compressIncludeMethod(HttpMethod.POST.name())
            .compressIncludeMethod(HttpMethod.PUT.name())
            .compressIncludeMethod(HttpMethod.PATCH.name())
            .compressIncludeMethod(HttpMethod.DELETE.name())
            .build();

        CompressionHandler handler = new CompressionHandler(next);
        handler.putCompression(gzip);
        handler.putConfiguration("/", config);
        return handler;
    }

    private static Handler cors(WebServerSettings settings, Handler next) {
        List<String> origins = settings.corsOrigins();
        if (origins.isEmpty()) {
            return next;
        }
        CrossOriginHandler handler = new CrossOriginHandler();
        handler.setAllowedOriginPatterns(Set.copyOf(origins));
        handler.setAllowedMethods(Arrays.stream(HttpMethod.values()).map(Enum::name).collect(Collectors.toSet()));
        handler.setAllowCredentials(false);
        handler.setHandler(next);
        return handler;
    }

    private static void stopQuietly(Server server) {
        try {
            server.stop();
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Error while stopping the CRN web API.", e);
        }
    }

    private static String basePathInfo() {
        return "/{" + String.join("|", NAMESPACES) + "}/" + API_SEGMENT + "/<version>";
    }
}
