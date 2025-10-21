package me.sathish.my_github_cleaner.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SathishLoggerClient {
    private final String baseUrl;
    private final String applicationName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public SathishLoggerClient(String baseUrl, String applicationName) {
        this(baseUrl, applicationName, null);
    }

    public SathishLoggerClient(String baseUrl, String applicationName, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.applicationName = applicationName;
        this.apiKey = apiKey;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // Synchronous logging methods
    public void info(String message) {
        log(LogLevel.INFO, message, null, null);
    }

    public void info(String message, String correlationId) {
        log(LogLevel.INFO, message, correlationId, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null, null);
    }

    public void warn(String message, String correlationId) {
        log(LogLevel.WARN, message, correlationId, null);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null, null);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, null, throwable);
    }

    public void error(String message, String correlationId, Throwable throwable) {
        log(LogLevel.ERROR, message, correlationId, throwable);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null, null);
    }

    public void debug(String message, String correlationId) {
        log(LogLevel.DEBUG, message, correlationId, null);
    }

    // Asynchronous logging methods
    public CompletableFuture<Void> infoAsync(String message) {
        return logAsync(LogLevel.INFO, message, null, null);
    }

    public CompletableFuture<Void> errorAsync(String message, Throwable throwable) {
        return logAsync(LogLevel.ERROR, message, null, throwable);
    }

    // Core logging method
    public void log(LogLevel level, String message, String correlationId, Throwable throwable) {
        try {
            logAsync(level, message, correlationId, throwable).get();
        } catch (Exception e) {
            // Fallback to console logging if remote logging fails
            System.err.println("Failed to send log to SathishLogger: " + e.getMessage());
            System.out.println(String.format(
                    "[%s] [%s] [%s] %s",
                    applicationName, correlationId != null ? correlationId : "N/A", level, message));
        }
    }

    // Asynchronous core logging method
    public CompletableFuture<Void> logAsync(LogLevel level, String message, String correlationId, Throwable throwable) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> logRequest = new HashMap<>();
                logRequest.put("applicationName", applicationName);
                logRequest.put("logLevel", level.name());
                logRequest.put("message", message);
                logRequest.put(
                        "correlationId",
                        correlationId != null
                                ? correlationId
                                : UUID.randomUUID().toString());
                logRequest.put("timestamp", LocalDateTime.now());
                logRequest.put("threadName", Thread.currentThread().getName());

                if (throwable != null) {
                    logRequest.put("exceptionMessage", throwable.getMessage());
                    logRequest.put("stackTrace", getStackTrace(throwable));
                }

                String jsonBody = objectMapper.writeValueAsString(logRequest);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/logs/log"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("X-API-Key", apiKey);
                }

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                }

            } catch (Exception e) {
                // Fallback to console logging
                System.err.println("Failed to send log to SathishLogger: " + e.getMessage());
                System.out.println(String.format(
                        "[%s] [%s] [%s] %s",
                        applicationName, correlationId != null ? correlationId : "N/A", level, message));
            }
        });
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName())
                .append(": ")
                .append(throwable.getMessage())
                .append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
}
