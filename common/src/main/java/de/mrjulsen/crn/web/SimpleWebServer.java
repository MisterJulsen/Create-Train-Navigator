package de.mrjulsen.crn.web;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.data.navigation.NavigatableGraph;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.google.common.net.MediaType;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

public class SimpleWebServer {

    static HttpServer server;

    public static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/hello", new HelloWorldHandler());
        server.createContext("/api/" + CreateRailwaysNavigator.MOD_ID + "/navigate", new V1NavigationHandler());
        server.createContext("/" + CreateRailwaysNavigator.MOD_ID, new WebsiteHandler(CreateRailwaysNavigator.MOD_ID));
        server.createContext("/" + CreateRailwaysNavigator.SHORT_MOD_ID, new WebsiteHandler(CreateRailwaysNavigator.SHORT_MOD_ID));
        server.createContext("/status", new TestHandler());
        server.setExecutor(null); // creates a default executor
        //server.start();
    }

    public static void stop() {
        DLUtils.doIfNotNull(server, x -> {
            x.stop(0);
        });
    }

    public static Map<String, List<String>> parseQueryParameters(HttpExchange ex, Charset charset) {
        String queryString = ex.getRequestURI().getRawQuery();
        if (queryString == null || queryString.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> parsedParams = new TreeMap<String, List<String>>();
        for (String param : queryString.split("&")) {
            String[] parts = param.split("=", 2);
            String key = parts[0];
            String value = parts.length == 2 ? parts[1] : "";
            try {
                key = URLDecoder.decode(key, charset.name());
                value = URLDecoder.decode(value, charset.name());
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
            List<String> values = parsedParams.get(key);
            if (values == null) {
                values = new LinkedList<String>();
                parsedParams.put(key, values);
            }
            values.add(value);
        }

        for (Map.Entry<String, List<String>> me : parsedParams.entrySet()) {
            me.setValue(Collections.unmodifiableList(me.getValue()));
        }
        return Collections.unmodifiableMap(parsedParams);
    }

    private static void startResponse(HttpExchange ex, int code, MediaType contentType, boolean hasBody) throws IOException {
        if (contentType != null) {
            ex.getResponseHeaders().set("Content-Type", contentType.type());
        }
        if (!hasBody) { // No body. Required for HEAD requests
            ex.sendResponseHeaders(code, -1);
        } else { // Chuncked encoding
            ex.sendResponseHeaders(code, 0);
        }
    }

    private static void sendError(HttpExchange ex, int code, String msg) {
        CreateRailwaysNavigator.LOGGER.warn(msg);
        try {
            respond(ex, code, MediaType.PLAIN_TEXT_UTF_8, msg.getBytes());
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to send error response.", e);
        }
    }

    private static void respond(HttpExchange ex, int code, MediaType contentType, byte response[]) throws IOException {
        startResponse(ex, code, contentType, response != null);
        if (response != null) {
            OutputStream responseBody = ex.getResponseBody();
            responseBody.write(response);
            responseBody.flush();
            responseBody.close();
        }
        ex.close();
    }

    public static void sendRedirect(HttpExchange ex, URI location) throws IOException {
        ex.getResponseHeaders().set("Location", location.toString());
        respond(ex, HttpURLConnection.HTTP_SEE_OTHER, null, null);
    }

    public static URI getRequestUri(HttpExchange ex) {
        String host = ex.getRequestHeaders().getFirst("Host");
        if (host == null) { // Client must be using HTTP/1.0
            CreateRailwaysNavigator.LOGGER.warn("Request did not provide Host header, using 'localhost' as hostname");
            int port = ex.getHttpContext().getServer().getAddress().getPort();
            host = "localhost:" + port;
        }
        String protocol = (ex.getHttpContext().getServer() instanceof HttpsServer) ? "https" : "http";
        URI base;
        try {
            base = new URI(protocol, host, "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        URI requestedUri = ex.getRequestURI();
        requestedUri = base.resolve(requestedUri);
        return requestedUri;
    }

    public static void redirectTo(HttpExchange ex, String redirect) {
        URI base = getRequestUri(ex);
        URI path;
        try {
            path = new URI(redirect);
            sendRedirect(ex, base.resolve(path));
        } catch (URISyntaxException | IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Could not construct URI.", e);
        }
    }



    static class HelloWorldHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Hello World! " + CreateRailwaysNavigator.MOD_ID;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class WebsiteHandler implements HttpHandler {

        private final String subUrl;
        private final int subUrlLength;

        public WebsiteHandler(String subUrl) {
            this.subUrl = subUrl;
            this.subUrlLength = subUrl.length() + 1;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            
            String requestedPath = t.getRequestURI().getPath();
            if (requestedPath.startsWith("/" + subUrl) && requestedPath.length() >= subUrlLength) {
                requestedPath = requestedPath.substring(subUrlLength);
            } else {
                sendError(t, HttpURLConnection.HTTP_BAD_REQUEST, "The requested URL is invalid: " + requestedPath);
            }

            if (requestedPath.isBlank()) {
                redirectTo(t, "/" + subUrl + "/");
                return;
            }

            if (requestedPath.equals("/")) {
                requestedPath = "/index.html";  // Set default page to index.html
            }

            CreateRailwaysNavigator.LOGGER.info("A web service requested a resource: " + requestedPath);
            Optional<byte[]> fileData = ModUtils.getWebsiteResourceManager().getFileBytesFor(requestedPath);

            if (!fileData.isPresent()) {
                sendError(t, HttpURLConnection.HTTP_NOT_FOUND, "The requested resource does not exist: " + requestedPath);
                /*
                String response = "404 (Not Found)\n" + requestedPath + " does not exist.";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                */
            } else {
                respond(t, HttpURLConnection.HTTP_OK, null, fileData.get());
                /*
                byte[] fileBytes = fileData.get();
                System.out.println("BYTES: " + fileBytes.length);
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.write(fileBytes);
                os.close();
                */
            }
        }
    }

    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "PACKS";
            for (String str :  ModCommonEvents.getCurrentServer().get().getPackRepository().getAvailableIds()) {
                response += "\n - " + str;
            }
            response += "\nCRN PACK";
            for (ResourceLocation str :  ModCommonEvents.getCurrentServer().get().getPackRepository().getPack("mod:" + CreateRailwaysNavigator.MOD_ID).open().getResources(PackType.SERVER_DATA, CreateRailwaysNavigator.MOD_ID, "", 100, (str) -> true)) {
                response += "\n - " + str;
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class V1NavigationHandler implements HttpHandler {

        private static final String KEY_START = "start";
        private static final String KEY_DESTINATION = "destination";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Map<String, List<String>> params = parseQueryParameters(t, StandardCharsets.UTF_8);

            if (!params.containsKey(KEY_START) || !params.containsKey(KEY_DESTINATION)) {
                sendError(t, HttpURLConnection.HTTP_BAD_REQUEST, "Wrong parameters.");
            }

            try {
                
                GlobalSettings settings = GlobalSettings.getInstance();
                List<Route> routes = NavigatableGraph.searchRoutes(settings.getOrCreateStationTagFor(params.get(KEY_START).get(0)), settings.getOrCreateStationTagFor(params.get(KEY_DESTINATION).get(0)), null, true);
                String response = DragonLib.GSON.toJson(routes);

                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("DEAD", e);
            }

            
        }
    }
}
