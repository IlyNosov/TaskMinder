package ru.ilynosov.taskminder.infrastructure.ai;

import org.junit.jupiter.api.Test;
import ru.ilynosov.taskminder.domain.model.Reminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicReminderMatchingServiceTest {

    private final HeuristicReminderMatchingService service = new HeuristicReminderMatchingService();

    @Test
    void matchesReminderByKeywordOverlap() {
        Reminder first = reminder("позвонить клиенту по договору");
        Reminder second = reminder("купить молоко и хлеб");

        var result = service.matchReminder(
                "удали напоминание позвонить клиенту",
                List.of(first, second)
        );

        assertTrue(result.isPresent());
        assertEquals(first.getId(), result.orElseThrow());
    }

    @Test
    void returnsEmptyForAmbiguousInput() {
        Reminder first = reminder("позвонить клиенту");
        Reminder second = reminder("позвонить поставщику");

        var result = service.matchReminder(
                "удали позвонить",
                List.of(first, second)
        );

        assertTrue(result.isEmpty());
    }

    private Reminder reminder(String text) {
        return new Reminder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                text,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now()
        );
    }
}
