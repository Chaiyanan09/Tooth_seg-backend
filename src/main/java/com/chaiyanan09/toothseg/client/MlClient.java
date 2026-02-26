package com.chaiyanan09.toothseg.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class MlClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public MlClient(@Value("${app.ml.baseUrl}") String baseUrl,
                    @Value("${app.ml.apiKey}") String apiKey) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String predictRawJson(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        System.out.println("[ML] send bytes length=" + (bytes == null ? -1 : bytes.length));

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Uploaded file bytes are empty (0 bytes).");
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/predict_raw"))
                .timeout(Duration.ofSeconds(180))
                .header("X-API-KEY", apiKey)
                .header("Content-Type", "application/octet-stream")
                .header("Accept", "application/json")
                .expectContinue(false)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("ML error HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
}