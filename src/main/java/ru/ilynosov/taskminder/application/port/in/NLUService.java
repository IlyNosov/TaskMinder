package ru.ilynosov.taskminder.application.port.in;

import ru.ilynosov.taskminder.domain.value.ParseResult;

public interface NLUService {

    ParseResult parse(String input);
}