package ru.ilynosov.taskminder.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.ilynosov.taskminder.infrastructure.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class OllamaClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public String chat(String systemPrompt, String userPrompt) {
        try {
            String url = AppConfig.getOllamaUrl() + "/api/generate";

            Map<String, Object> body = Map.of(
                    "model", AppConfig.getOllamaModel(),
                    "prompt", systemPrompt + "\n\n" + userPrompt,
                    "stream", false,
                    "temperature", 0
            );

            String json = mapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 300) {
                throw new RuntimeException("Ollama HTTP " + resp.statusCode() + ": " + resp.body());
            }

            // Ollama /api/generate возвращает JSON, поле "response" содержит текст
            return mapper.readTree(resp.body()).get("response").asText();

        } catch (Exception e) {
            throw new RuntimeException("Ollama call failed: " + e.getMessage(), e);
        }
    }
}