package ru.ilynosov.taskminder.application.port.out;

import ru.ilynosov.taskminder.domain.model.Reminder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LlmMatchingPort {

    Optional<UUID> matchReminder(
            String userInput,
            List<Reminder> activeReminders
    );
}