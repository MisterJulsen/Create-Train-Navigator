package de.mrjulsen.crn.web.api;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import de.mrjulsen.crn.api.json.JsonConvert;

public final class Response {

    private final int status;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body;
    private Object jsonPayload;
    private String contentType;

    private Response(int status) {
        this.status = status;
    }

    public static Response status(int status) {
        return new Response(status);
    }

    public static Response ok() {
        return new Response(HttpURLConnection.HTTP_OK);
    }

    public static Response noContent() {
        return new Response(HttpURLConnection.HTTP_NO_CONTENT);
    }

    public static Response json(Object data) {
        return json(HttpURLConnection.HTTP_OK, data);
    }

    public static Response json(int status, Object data) {
        Response response = new Response(status);
        response.jsonPayload = data;
        response.contentType = MediaType.JSON;
        return response;
    }

    public static Response created(Object data) {
        return json(HttpURLConnection.HTTP_CREATED, data);
    }

    public static Response text(String text) {
        return new Response(HttpURLConnection.HTTP_OK)
            .rawBody(text.getBytes(StandardCharsets.UTF_8), MediaType.TEXT);
    }

    public static Response error(int status, String message) {
        return json(status, new ErrorBody(status, message));
    }

    public Response header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Response contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Response rawBody(byte[] body, String contentType) {
        this.body = body;
        this.jsonPayload = null;
        this.contentType = contentType;
        return this;
    }

    public boolean isJson() {
        return jsonPayload != null;
    }

    public Object jsonPayload() {
        return jsonPayload;
    }

    public Response jsonPayload(Object payload) {
        this.jsonPayload = payload;
        return this;
    }

    public int status() {
        return status;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public byte[] body() {
        if (body == null && jsonPayload != null) {
            body = JsonConvert.toJson(jsonPayload).getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    public String contentType() {
        return contentType;
    }



    private record ErrorBody(int status, String error) {}
}
