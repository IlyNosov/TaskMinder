package ru.ilynosov.taskminder.application.port.out;

public interface LlmRewritePort {
    String rewrite(String input);
}
