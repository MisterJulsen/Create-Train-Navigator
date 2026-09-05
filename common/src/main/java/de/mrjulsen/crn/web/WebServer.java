package de.mrjulsen.crn.web;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import org.jetbrains.annotations.NotNull;

public final class WebServer {

    public static final String API_SEGMENT = "api";
    public static final List<String> NAMESPACES = List.of(
        CreateRailwaysNavigator.MOD_ID,
        CreateRailwaysNavigator.SHORT_MOD_ID
    );

    private static final int STOP_DELAY_SECONDS = 0;

    private static WebServer instance;

    private final List<HttpServer> listeners = new ArrayList<>();
    private ExecutorService executor;

    private WebServer() {}

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

        WebServer server = new WebServer();
        try {
            server.startListeners(settings);
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Failed to start the CRN web API.", e);
            server.shutdown();
            return;
        }
        if (server.listeners.isEmpty()) {
            server.shutdown();
            return;
        }
        instance = server;
    }

    public static synchronized void stop() {
        if (instance == null) {
            return;
        }
        instance.shutdown();
        instance = null;
        CreateRailwaysNavigator.LOGGER.info("CRN web API stopped.");
    }



    private void startListeners(WebServerSettings settings) throws Exception {
        executor = Executors.newFixedThreadPool(settings.threads(), new WebThreadFactory());
        ApiRouter router = new ApiRouter(settings);
        InetAddress bindAddress = InetAddress.getByName(settings.bindAddress());

        HttpServer http = HttpServer.create(new InetSocketAddress(bindAddress, settings.port()), 0);
        configure(http, router);
        http.start();
        listeners.add(http);
        CreateRailwaysNavigator.LOGGER.info("CRN web API listening on {}:{}{}", settings.bindAddress(), settings.port(), basePathInfo());
        CreateRailwaysNavigator.LOGGER.info("CRN web API serving {} endpoint(s).", EndpointRegistry.size());
    }

    private void configure(HttpServer server, ApiRouter router) {
        for (String namespace : NAMESPACES) {
            server.createContext("/" + namespace + "/" + API_SEGMENT, router);
        }
        server.setExecutor(executor);
    }

    private static String basePathInfo() {
        return "/{" + String.join("|", NAMESPACES) + "}/" + API_SEGMENT + "/<version>";
    }

    private void shutdown() {
        for (HttpServer listener : listeners) {
            listener.stop(STOP_DELAY_SECONDS);
        }
        listeners.clear();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }




    private static final class WebThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "CRN Web #" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
