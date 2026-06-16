package ru.ilynosov.taskminder.infrastructure.ai;

import ru.ilynosov.taskminder.application.port.in.NLUService;
import ru.ilynosov.taskminder.application.port.out.LlmRewritePort;
import ru.ilynosov.taskminder.domain.value.ParseResult;

public class NLUServiceImpl implements NLUService {

    private final RuleBasedParser ruleParser;
    private final LlmRewritePort llmRewritePort;

    public NLUServiceImpl(RuleBasedParser ruleParser,
                          LlmRewritePort llmRewritePort) {
        this.ruleParser = ruleParser;
        this.llmRewritePort = llmRewritePort;
    }

    @Override
    public ParseResult parse(String input) {

        ParseResult result = ruleParser.parse(input);

        if (result.getConfidence() >= 0.7)
            return result;

        String rewritten;
        try {
            rewritten = llmRewritePort.rewrite(input);
        } catch (Exception e) {
            return result;
        }

        ParseResult retry = ruleParser.parse(rewritten);

        if (retry.getConfidence() > result.getConfidence())
            return retry;

        return result;
    }
}
