package ru.ilynosov.taskminder.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderTest {

    @Test
    void createsActiveReminderForFutureDate() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);

        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Позвонить клиенту",
                scheduledAt,
                LocalDateTime.now()
        );

        assertEquals("Позвонить клиенту", reminder.getText());
        assertEquals(scheduledAt, reminder.getScheduledAt());
        assertEquals(ReminderStatus.ACTIVE, reminder.getStatus());
        assertTrue(reminder.isActive());
    }

    @Test
    void rejectsPastDateOnCreate() {
        assertThrows(IllegalArgumentException.class, () -> new Reminder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Просроченная задача",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now()
        ));
    }

    @Test
    void restoresReminderFromDatabaseWithoutFutureDateValidation() {
        LocalDateTime scheduledAt = LocalDateTime.now().minusDays(1);

        Reminder reminder = Reminder.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Старая задача",
                scheduledAt,
                ReminderStatus.DONE,
                LocalDateTime.now().minusDays(2)
        );

        assertEquals(ReminderStatus.DONE, reminder.getStatus());
        assertEquals(scheduledAt, reminder.getScheduledAt());
        assertFalse(reminder.isActive());
    }

    @Test
    void updatesMutableFields() {
        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Старая формулировка",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now()
        );
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);

        reminder.updateText("Новая формулировка");
        reminder.reschedule(newDate);
        reminder.setStatus(ReminderStatus.CANCELLED);

        assertEquals("Новая формулировка", reminder.getText());
        assertEquals(newDate, reminder.getScheduledAt());
        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());
        assertFalse(reminder.isActive());
    }
}
