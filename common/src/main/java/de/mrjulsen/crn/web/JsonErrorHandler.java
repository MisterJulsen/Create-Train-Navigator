package de.mrjulsen.crn.web;

import java.net.HttpURLConnection;
import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.api.HttpHeader;

final class JsonErrorHandler implements Request.Handler {

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        int status = response.getStatus() >= 400 ? response.getStatus() : HttpURLConnection.HTTP_NOT_FOUND;
        ApiResult result = ApiResult.error(status, messageFor(status, request));
        response.setStatus(status);
        if (result.contentType() != null) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, result.contentType());
        }
        response.write(true, ByteBuffer.wrap(result.body()), callback);
        return true;
    }

    private static String messageFor(int status, Request request) {
        return switch (status) {
            case HttpURLConnection.HTTP_NOT_FOUND -> "No endpoint for " + request.getHttpURI().getPath();
            case HttpURLConnection.HTTP_BAD_METHOD -> "Method not allowed: " + request.getMethod();
            case HttpURLConnection.HTTP_BAD_REQUEST -> "Bad request";
            case HttpURLConnection.HTTP_INTERNAL_ERROR -> "Internal server error";
            default -> "Request failed";
        };
    }
}
