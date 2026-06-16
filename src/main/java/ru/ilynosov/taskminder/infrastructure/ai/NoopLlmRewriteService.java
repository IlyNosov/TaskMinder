package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.application.port.out.LlmRewritePort;

public class NoopLlmRewriteService implements LlmRewritePort {

    @Override
    public String rewrite(String input) {
        return input;
    }
}
