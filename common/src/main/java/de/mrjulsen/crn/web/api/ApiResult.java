package de.mrjulsen.crn.web.api;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import de.mrjulsen.crn.api.json.JsonConvert;

public final class ApiResult {

    private final int status;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body;
    private Object jsonPayload;
    private String contentType;

    private ApiResult(int status) {
        this.status = status;
    }

    public static ApiResult status(int status) {
        return new ApiResult(status);
    }

    public static ApiResult ok() {
        return new ApiResult(HttpURLConnection.HTTP_OK);
    }

    public static ApiResult noContent() {
        return new ApiResult(HttpURLConnection.HTTP_NO_CONTENT);
    }

    public static ApiResult json(Object data) {
        return json(HttpURLConnection.HTTP_OK, data);
    }

    public static ApiResult json(int status, Object data) {
        ApiResult result = new ApiResult(status);
        result.jsonPayload = data;
        result.contentType = MediaType.JSON;
        return result;
    }

    public static ApiResult created(Object data) {
        return json(HttpURLConnection.HTTP_CREATED, data);
    }

    public static ApiResult text(String text) {
        return new ApiResult(HttpURLConnection.HTTP_OK)
            .rawBody(text.getBytes(StandardCharsets.UTF_8), MediaType.TEXT);
    }

    public static ApiResult error(int status, String message) {
        return json(status, new ErrorBody(status, message));
    }

    public ApiResult header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ApiResult contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ApiResult rawBody(byte[] body, String contentType) {
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

    public ApiResult jsonPayload(Object payload) {
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
