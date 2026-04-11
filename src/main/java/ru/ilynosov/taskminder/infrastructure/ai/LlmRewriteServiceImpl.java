package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.application.port.out.LlmRewritePort;

public class LlmRewriteServiceImpl implements LlmRewritePort {

    private final OllamaClient client;

    public LlmRewriteServiceImpl(OllamaClient client) {
        this.client = client;
    }

    @Override
    public String rewrite(String input) {

        String system = """
            Rewrite the user's message into a clean,
            structured Russian reminder sentence.
            Preserve meaning.
            Do NOT add information.
            """;

        return client.chat(system, input);
    }
}
