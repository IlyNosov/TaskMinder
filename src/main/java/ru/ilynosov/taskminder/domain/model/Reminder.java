package ru.ilynosov.taskminder.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Reminder {

    private final UUID id;
    private final UUID userId;
    private final String text;
    private final LocalDateTime scheduledAt;
    private ReminderStatus status;
    private final LocalDateTime createdAt;

    public Reminder(UUID id,
                    UUID userId,
                    String text,
                    LocalDateTime scheduledAt,
                    LocalDateTime createdAt) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Reminder text cannot be empty");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create reminder in the past");
        }

        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.text = text.trim();
        this.scheduledAt = Objects.requireNonNull(scheduledAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = ReminderStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markDone() {
        this.status = ReminderStatus.DONE;
    }

    public void cancel() {
        this.status = ReminderStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == ReminderStatus.ACTIVE;
    }
}