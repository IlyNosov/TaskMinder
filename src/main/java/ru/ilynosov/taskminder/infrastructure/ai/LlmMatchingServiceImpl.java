package ru.ilynosov.taskminder.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.ilynosov.taskminder.application.port.out.LlmMatchingPort;
import ru.ilynosov.taskminder.domain.model.Reminder;
import ru.ilynosov.taskminder.infrastructure.ai.dto.LlmMatchResponse;

import java.util.*;
import java.util.stream.Collectors;

public class LlmMatchingServiceImpl implements LlmMatchingPort {

    private final OllamaClient ollamaClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmMatchingServiceImpl(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public Optional<UUID> matchReminder(String userInput, List<Reminder> activeReminders) {

        if (activeReminders == null || activeReminders.isEmpty()) {
            return Optional.empty();
        }

        // Сжимаем контекст: id + text + time
        String items = activeReminders.stream()
                .map(r -> String.format(
                        "{\"id\":\"%s\",\"text\":\"%s\",\"time\":\"%s\"}",
                        r.getId(),
                        escape(r.getText()),
                        r.getScheduledAt()
                ))
                .collect(Collectors.joining(",\n"));

        String system = """
                You are an assistant that selects which reminder the user refers to.
                Return ONLY a valid JSON object with fields:
                - matched_id: UUID string from the list OR "none"
                - confidence: number from 0 to 1
                No extra text, no markdown.
                """;

        String user = """
                User message: "%s"
                Active reminders (JSON array):
                [
                %s
                ]
                
                Rules:
                - Choose the single best matching reminder.
                - If ambiguous or no good match, return matched_id="none" and confidence<=0.5
                """.formatted(userInput, items);

        String raw = ollamaClient.chat(system, user).trim();

        // Иногда модель может добавить мусор
        String json = extractJsonObject(raw);

        try {
            LlmMatchResponse resp = mapper.readValue(json, LlmMatchResponse.class);

            if (resp.matched_id == null) return Optional.empty();
            if ("none".equalsIgnoreCase(resp.matched_id)) return Optional.empty();
            if (resp.confidence < 0.6) return Optional.empty();

            return Optional.of(UUID.fromString(resp.matched_id));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractJsonObject(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s; // пусть упадет выше
    }
}