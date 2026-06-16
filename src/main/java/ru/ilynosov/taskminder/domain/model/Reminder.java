package ru.ilynosov.taskminder.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Reminder {

    private final UUID id;
    private final UUID userId;
    private String text;
    private LocalDateTime scheduledAt;
    private ReminderStatus status;
    private final LocalDateTime createdAt;

    public Reminder(UUID id,
                    UUID userId,
                    String text,
                    LocalDateTime scheduledAt,
                    LocalDateTime createdAt) {
        this(id, userId, text, scheduledAt, ReminderStatus.ACTIVE, createdAt, true);
    }

    private Reminder(UUID id,
                     UUID userId,
                     String text,
                     LocalDateTime scheduledAt,
                     ReminderStatus status,
                     LocalDateTime createdAt,
                     boolean validateScheduledAt) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Reminder text cannot be empty");
        }

        if (validateScheduledAt && scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create reminder in the past");
        }

        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.text = text.trim();
        this.scheduledAt = Objects.requireNonNull(scheduledAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
    }

    public static Reminder restore(UUID id,
                                   UUID userId,
                                   String text,
                                   LocalDateTime scheduledAt,
                                   ReminderStatus status,
                                   LocalDateTime createdAt) {
        return new Reminder(id, userId, text, scheduledAt, status, createdAt, false);
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

    public void updateText(String newText) {
        if (newText == null || newText.isBlank()) {
            throw new IllegalArgumentException("Reminder text cannot be empty");
        }

        this.text = newText.trim();
    }

    public void reschedule(LocalDateTime newScheduledAt) {
        if (newScheduledAt == null) {
            throw new IllegalArgumentException("Scheduled time cannot be empty");
        }

        this.scheduledAt = newScheduledAt;
    }

    public void setStatus(ReminderStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
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
