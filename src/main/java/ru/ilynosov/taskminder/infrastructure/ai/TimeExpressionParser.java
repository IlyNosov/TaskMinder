package ru.ilynosov.taskminder.infrastructure.ai;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeExpressionParser {

    private static final Map<String, DayOfWeek> WEEKDAYS = Map.of(
            "понедельник", DayOfWeek.MONDAY,
            "вторник", DayOfWeek.TUESDAY,
            "среду", DayOfWeek.WEDNESDAY,
            "среда", DayOfWeek.WEDNESDAY,
            "четверг", DayOfWeek.THURSDAY,
            "пятницу", DayOfWeek.FRIDAY,
            "пятница", DayOfWeek.FRIDAY,
            "субботу", DayOfWeek.SATURDAY,
            "суббота", DayOfWeek.SATURDAY,
            "воскресенье", DayOfWeek.SUNDAY
    );

    public LocalDateTime parse(String input) {

        input = input.toLowerCase();

        LocalDateTime relative = parseRelative(input);
        if (relative != null) return relative;

        LocalDateTime absolute = parseAbsolute(input);
        if (absolute != null) return absolute;

        LocalDateTime weekday = parseWeekday(input);
        if (weekday != null) return weekday;

        return null;
    }

    private LocalDateTime parseRelative(String input) {

        Pattern pattern = Pattern.compile(
                "через (\\d+) (секунд|секунды|секунду|минут|минуты|минута|час|часа|часов|день|дня|дней|недел[яи])"
        );

        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {

            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            LocalDateTime now = LocalDateTime.now();

            if (unit.startsWith("сек")) return now.plusSeconds(amount);
            if (unit.startsWith("мин")) return now.plusMinutes(amount);
            if (unit.startsWith("час")) return now.plusHours(amount);
            if (unit.startsWith("ден") || unit.startsWith("дн")) return now.plusDays(amount);
            if (unit.startsWith("нед")) return now.plusWeeks(amount);
        }

        return null;
    }

    private LocalDateTime parseAbsolute(String input) {

        // сегодня / завтра / послезавтра
        Pattern pattern = Pattern.compile(
                "(сегодня|завтра|послезавтра)? ?в (\\d{1,2})(:(\\d{2}))?"
        );

        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {

            String dayWord = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(4) != null
                    ? Integer.parseInt(matcher.group(4))
                    : 0;

            LocalDate date = LocalDate.now();

            if ("завтра".equals(dayWord))
                date = date.plusDays(1);
            else if ("послезавтра".equals(dayWord))
                date = date.plusDays(2);

            return LocalDateTime.of(date, LocalTime.of(hour, minute));
        }

        return null;
    }

    private LocalDateTime parseWeekday(String input) {

        for (String key : WEEKDAYS.keySet()) {

            if (input.contains(key)) {

                DayOfWeek target = WEEKDAYS.get(key);

                Pattern timePattern = Pattern.compile("в (\\d{1,2})(:(\\d{2}))?");
                Matcher timeMatcher = timePattern.matcher(input);

                int hour = 9;
                int minute = 0;

                if (timeMatcher.find()) {
                    hour = Integer.parseInt(timeMatcher.group(1));
                    minute = timeMatcher.group(3) != null
                            ? Integer.parseInt(timeMatcher.group(3))
                            : 0;
                }

                LocalDate next = LocalDate.now()
                        .with(TemporalAdjusters.nextOrSame(target));

                return LocalDateTime.of(next, LocalTime.of(hour, minute));
            }
        }

        return null;
    }
}