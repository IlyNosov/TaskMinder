package ru.ilynosov.taskminder.infrastructure.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}