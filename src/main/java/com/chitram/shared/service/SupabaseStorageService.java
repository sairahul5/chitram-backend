package com.chitram.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final HttpClient httpClient;

    public SupabaseStorageService(
            @Value("${supabase.url:https://vioobzddncnyqbfljqhp.supabase.co}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:chitram-images}") String bucket) {
        this.supabaseUrl = supabaseUrl.replaceAll("/$", "");
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String upload(byte[] content, String storagePath, String contentType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(objectUrl(storagePath)))
                    .timeout(Duration.ofSeconds(30))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Supabase storage upload failed: HTTP {} - {}", response.statusCode(), response.body());
                throw new IllegalStateException("Failed to upload to Supabase storage: " + response.body());
            }
            return publicUrl(storagePath);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to upload image to Supabase Storage", exception);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(objectUrl(storagePath)))
                    .timeout(Duration.ofSeconds(15))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully deleted image object from Supabase: {}", storagePath);
            } else {
                log.warn("Supabase returned status {} when deleting {}: {}", response.statusCode(), storagePath,
                        response.body());
            }
        } catch (Exception exception) {
            log.error("Could not delete file from Supabase storage: {}", storagePath, exception);
        }
    }

    private String objectUrl(String storagePath) {
        return String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, storagePath);
    }

    private String publicUrl(String storagePath) {
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, storagePath);
    }
}
