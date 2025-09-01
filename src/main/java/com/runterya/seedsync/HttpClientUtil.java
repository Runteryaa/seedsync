package com.seed_sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpClientUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static void postJson(String url, Map<String, Object> body) throws Exception {
        String json = GSON.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + " from sync endpoint: " + response.body());
        } else {
            System.out.println("[SeedSync] Synced worlds successfully. Server said: " + response.body());
        }
    }
}
