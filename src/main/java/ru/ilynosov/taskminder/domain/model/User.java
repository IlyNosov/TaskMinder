package ru.ilynosov.taskminder.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final long telegramId;
    private final String timezone;
    private final LocalDateTime createdAt;

    public User(UUID id, long telegramId, String timezone, LocalDateTime createdAt) {
        if (telegramId <= 0) {
            throw new IllegalArgumentException("Telegram ID must be positive");
        }

        this.id = Objects.requireNonNull(id);
        this.telegramId = telegramId;
        this.timezone = timezone == null ? "UTC" : timezone;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() {
        return id;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}