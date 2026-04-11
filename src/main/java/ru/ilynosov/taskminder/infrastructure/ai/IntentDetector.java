package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.domain.value.Intent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentDetector {

    public Intent detect(String input) {

        String text = input.toLowerCase();

        if (text.matches(".*(покажи|список|что у меня|какие у меня|покажи задачи).*"))
            return Intent.LIST;

        if (text.matches(".*(удали|отмени|удалить|отменить).*"))
            return Intent.DELETE;

        if (text.matches(".*(напомни|напомнить|пни|скажи|через|сегодня|завтра|в ).*"))
            return Intent.CREATE;

        return Intent.UNKNOWN;
    }

    public Integer extractDeleteIndex(String input) {

        Pattern pattern = Pattern.compile("(удали|отмени).*?(\\d+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(2));
        }

        return null;
    }
}