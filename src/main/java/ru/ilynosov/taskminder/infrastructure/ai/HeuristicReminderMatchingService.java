package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.application.port.out.LlmMatchingPort;
import ru.ilynosov.taskminder.domain.model.Reminder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeuristicReminderMatchingService implements LlmMatchingPort {

    private static final Set<String> STOP_WORDS = Set.of(
            "удали", "удалить", "отмени", "отменить", "задачу", "напоминание",
            "напоминаниею", "пожалуйста", "мне", "мой", "мою", "мое", "моё"
    );

    @Override
    public Optional<UUID> matchReminder(String userInput, List<Reminder> activeReminders) {
        if (userInput == null || userInput.isBlank() || activeReminders == null || activeReminders.isEmpty()) {
            return Optional.empty();
        }

        Set<String> inputTokens = tokenize(userInput);
        if (inputTokens.isEmpty()) {
            return Optional.empty();
        }

        List<ScoredReminder> scored = activeReminders.stream()
                .map(reminder -> new ScoredReminder(reminder, score(inputTokens, tokenize(reminder.getText()))))
                .filter(item -> item.score > 0)
                .sorted(Comparator.comparingDouble(ScoredReminder::score).reversed())
                .toList();

        if (scored.isEmpty()) {
            return Optional.empty();
        }

        if (scored.size() > 1 && scored.get(0).score == scored.get(1).score) {
            return Optional.empty();
        }

        if (scored.get(0).score < 0.34d) {
            return Optional.empty();
        }

        return Optional.of(scored.get(0).reminder.getId());
    }

    private static double score(Set<String> inputTokens, Set<String> reminderTokens) {
        if (reminderTokens.isEmpty()) {
            return 0;
        }

        long matches = inputTokens.stream()
                .filter(reminderTokens::contains)
                .count();

        return (double) matches / reminderTokens.size();
    }

    private static Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 1)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private record ScoredReminder(Reminder reminder, double score) {
    }
}
