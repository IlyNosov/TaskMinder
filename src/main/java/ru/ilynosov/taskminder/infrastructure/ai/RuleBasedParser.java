package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.domain.value.*;

import java.time.LocalDateTime;

public class RuleBasedParser {

    private final TimeExpressionParser timeParser;
    private final IntentDetector intentDetector;

    public RuleBasedParser(TimeExpressionParser timeParser,
                           IntentDetector intentDetector) {
        this.timeParser = timeParser;
        this.intentDetector = intentDetector;
    }

    public ParseResult parse(String input) {

        if (input == null || input.isBlank())
            return unknown(input);

        String normalized = input.toLowerCase();

        Intent intent = intentDetector.detect(normalized);

        switch (intent) {

            case LIST:
                return new ParseResult(
                        Intent.LIST,
                        null,
                        null,
                        null,
                        0.9,
                        false,
                        false,
                        null,
                        input
                );

            case DELETE:

                Integer index = intentDetector.extractDeleteIndex(normalized);

                if (index == null)
                    return unknown(input);

                return new ParseResult(
                        Intent.DELETE,
                        null,
                        null,
                        null,
                        0.9,
                        false,
                        false,
                        index,
                        input
                );

            case CREATE:
                return handleCreate(normalized);

            default:
                return unknown(input);
        }
    }

    private ParseResult handleCreate(String input) {

        var dateTime = timeParser.parse(input);

        if (dateTime == null)
            return unknown(input);

        String cleaned = cleanText(input);

        return new ParseResult(
                Intent.CREATE,
                dateTime.toLocalDate(),
                dateTime.toLocalTime(),
                cleaned,
                0.85,
                false,
                false,
                null,
                input
        );
    }

    private String cleanText(String input) {
        return input
                .replaceAll("через .*", "")
                .replaceAll("(сегодня|завтра|послезавтра)? ?в \\d{1,2}(:\\d{2})?", "")
                .replaceAll("в (понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье)", "")
                .replaceAll("напомни|напомнить|пни|скажи", "")
                .trim();
    }

    private ParseResult unknown(String input) {
        return new ParseResult(
                Intent.UNKNOWN,
                null,
                null,
                null,
                0.0,
                false,
                true,
                null,
                input
        );
    }
}