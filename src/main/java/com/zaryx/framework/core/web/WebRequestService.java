package com.zaryx.framework.core.web;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WebRequestService {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final Executor executor;

    public WebRequestService(int connectTimeoutMs, int readTimeoutMs, Executor executor) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.executor = executor;
    }

    public WebResponse get(String url) {
        return execute("GET", url, null, Collections.emptyMap());
    }

    public WebResponse delete(String url) {
        return execute("DELETE", url, null, Collections.emptyMap());
    }

    public WebResponse post(String url, String body) {
        return execute("POST", url, body, Collections.emptyMap());
    }

    public WebResponse put(String url, String body) {
        return execute("PUT", url, body, Collections.emptyMap());
    }

    public CompletableFuture<WebResponse> getAsync(String url) {
        return CompletableFuture.supplyAsync(() -> get(url), executor);
    }

    public CompletableFuture<WebResponse> postAsync(String url, String body) {
        return CompletableFuture.supplyAsync(() -> post(url, body), executor);
    }

    public WebResponse execute(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            if (body != null && !body.isEmpty() && allowsBody(method)) {
                connection.setDoOutput(true);
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(payload);
            }

            int status = connection.getResponseCode();
            String responseBody = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            return new WebResponse(status, responseBody, new LinkedHashMap<>(connection.getHeaderFields()));
        } catch (Exception e) {
            throw new IllegalStateException("HTTP request failed: " + method + " " + url, e);
        }
    }

    private boolean allowsBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private String readBody(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    public static class WebResponse {
        private final int statusCode;
        private final String body;
        private final Map<String, java.util.List<String>> headers;

        public WebResponse(int statusCode, String body, Map<String, java.util.List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.headers = headers;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public Map<String, java.util.List<String>> getHeaders() {
            return headers;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
